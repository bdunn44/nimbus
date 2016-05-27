package com.kbdunn.nimbus.server.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileDeleteEvent;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.FileUpdateEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.common.exception.FileConflictException;
import com.kbdunn.nimbus.common.model.FileConflict;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.util.ComparatorUtil;
import com.kbdunn.nimbus.common.util.FileUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.api.async.EventBus;
import com.kbdunn.nimbus.server.dao.NimbusFileDAO;

public class FileSyncService {

	private static final Logger log = LogManager.getLogger(FileSyncService.class);
	
	private final ExecutorService hashExecutor;
	private LocalUserService userService;
	private LocalFileService fileService;
	
	public FileSyncService() { 
		hashExecutor = Executors.newSingleThreadExecutor();
	}
	
	public void initialize(NimbusContext context) {
		userService = context.getUserService();
		fileService = context.getFileService();
	}
	
	private void publishFileEvent(NimbusUser user, FileEvent event) {
		EventBus.pushFileEvent(user, event);
	}
	
	public void publishFileAddEvent(NimbusFile file) throws IOException {
		publishFileAddEvent(file, null);
	}
	
	public void publishFileAddEvent(NimbusFile file, String originationId) throws IOException {
		final NimbusUser user = getFileUser(file);
		if (!isTrackedFile(user, file)) return; // Don't worry about files not in the sync folder
		
		if (!file.isDirectory()) {
			hashExecutor.submit(() -> {
				try {
					if (!hashIfNeeded(file)) log.warn("Hash was not updated for a file add event!");
					FileAddEvent event = new FileAddEvent(toSyncFile(user, file));
					if (originationId != null) event.setOriginationId(originationId);
					publishFileEvent(user, event);
				} catch (IOException e) {
					log.error("Error hashing updated file", e);
				}
			});
		} else {
			FileAddEvent event = new FileAddEvent(toSyncFile(user, file));
			if (originationId != null) event.setOriginationId(originationId);
			publishFileEvent(user, event);
		}
	}

	public void publishFileDeleteEvent(NimbusFile file) throws IOException {
		publishFileDeleteEvent(file, null);
	}
	
	public void publishFileDeleteEvent(NimbusFile file, String originationId) throws IOException {
		final NimbusUser user = getFileUser(file);
		final SyncFile syncFile = toSyncFile(user, file);
		if (syncFile == null) return; // Don't worry about files not in the sync folder
		FileDeleteEvent event = new FileDeleteEvent(syncFile);
		if (originationId != null) event.setOriginationId(originationId);
		publishFileEvent(user, event);
	}
	
	public void publishFileMoveEvent(NimbusFile moveSource, NimbusFile moveTarget) throws IOException {
		publishFileCopyOrMoveEvent(moveSource, moveTarget, true, null);
	}
	
	public void publishFileCopyEvent(NimbusFile copySource, NimbusFile copyTarget) throws IOException {
		publishFileCopyOrMoveEvent(copySource, copyTarget, false, null);
	}
	
	public void publishFileMoveEvent(NimbusFile moveSource, NimbusFile moveTarget, String originationId) throws IOException {
		publishFileCopyOrMoveEvent(moveSource, moveTarget, true, originationId);
	}
	
	public void publishFileCopyEvent(NimbusFile copySource, NimbusFile copyTarget, String originationId) throws IOException {
		publishFileCopyOrMoveEvent(copySource, copyTarget, false, originationId);
	}
	
	public void publishFileCopyOrMoveEvent(NimbusFile copySource, NimbusFile copyTarget, boolean move, String originationId) throws IOException {
		final NimbusUser user = getFileUser(copyTarget);
		final SyncFile srcSyncFile = toSyncFile(user, copySource);
		final SyncFile dstSyncFile = toSyncFile(user, copyTarget);
		if (srcSyncFile == null && dstSyncFile == null) {
			// Nothing to do
			return;
		} else if (srcSyncFile == null) {
			// Source came from outside the sync folder, it's an add event
			publishFileAddEvent(copyTarget, originationId);
		} else if (dstSyncFile == null) {
			// Destination file is outside the sync folder. 
			// If it's a move, delete the source file.
			if (move) publishFileDeleteEvent(copySource, originationId);
		} else {
			// Operation was within sync folder
			final FileEvent event = move ? 
					new FileMoveEvent(srcSyncFile, dstSyncFile) : 
					new FileCopyEvent(srcSyncFile, dstSyncFile);
			if (originationId != null) event.setOriginationId(originationId);
			publishFileEvent(user, event);
		}
	}

	public void publishFileUpdateEvent(NimbusFile file) {
		publishFileUpdateEvent(file, null);
	}
	
	public void publishFileUpdateEvent(NimbusFile file, String originationId) {
		if (file.isDirectory()) {
			log.warn("Cannot trigger file update event for a directory");
			return;
		}
		
		final NimbusUser user = getFileUser(file);
		if (!isTrackedFile(user, file)) return; // Don't worry about files not in the sync folder
		
		// Check if the file size changed, or if the file was modified since the last record save
		hashExecutor.submit(() -> {
			try {
				if (hashIfNeeded(file)) {
					FileUpdateEvent event = new FileUpdateEvent(toSyncFile(user, file));
					if (originationId != null) event.setOriginationId(originationId);
					publishFileEvent(user, event);
				}
			} catch (IOException e) {
				log.error("Error hashing updated file", e);
			}
		});
	}
	
	public List<SyncFile> getSyncFileList(NimbusUser user) throws IOException {
		final List<SyncFile> syncFiles = new ArrayList<>();
		final NimbusFile syncRoot = userService.getSyncRootFolder(user);
		for (NimbusFile file : fileService.getRecursiveFolderContents(syncRoot)) {
			if (hashIfNeeded(file)) {
				// Get updated hash
				file = fileService.getFileById(file.getId());
			}
			syncFiles.add(SyncFileUtil.toSyncFile(syncRoot, file));
		}
		return syncFiles;
	}
	
	public NimbusFile processCreateDirectory(NimbusUser user, FileAddEvent event) {
		if (!event.getFile().isDirectory()) throw new IllegalArgumentException("File is not a directory: " + event);
		final NimbusFile folder = toNimbusFile(user, event.getFile());
		if (fileService.createDirectory(folder, event.getOriginationId())) {
			return folder;
		}
		return null;
	}
	
	public NimbusFile processFileMove(NimbusUser user, FileMoveEvent event) throws FileConflictException {
		final NimbusFile source = toNimbusFile(user, event.getSrcFile());
		final NimbusFile target = toNimbusFile(user, event.getDstFile());
		try {
			return fileService.moveFile(source, target.getPath(), event.getOriginationId());
		} catch (FileConflictException e) {
			if (!event.isReplaceExistingFile()) throw e;
			
			List<FileConflict> conflicts = e.getConflicts();
			for (FileConflict conflict : conflicts) {
				conflict.setResolution(FileConflict.Resolution.REPLACE);
			}
			if (fileService.processBatchCopyOrMove(Collections.singletonList(source), 
					fileService.getParentFile(target), conflicts, true, event.getOriginationId())) {
				return fileService.getFileByPath(target.getPath());
			} else {
				throw e;
			}
		}
	}
	
	public NimbusFile processFileCopy(NimbusUser user, FileCopyEvent event) throws FileConflictException {
		final NimbusFile source = toNimbusFile(user, event.getSrcFile());
		final NimbusFile target = toNimbusFile(user, event.getDstFile());
		try {
			return fileService.copyFile(source, target.getPath(), event.getOriginationId());
		} catch (FileConflictException e) {
			if (!event.isReplaceExistingFile()) throw e;
			
			List<FileConflict> conflicts = e.getConflicts();
			for (FileConflict conflict : conflicts) {
				conflict.setResolution(FileConflict.Resolution.REPLACE);
			}
			if (fileService.processBatchCopyOrMove(Collections.singletonList(source), 
					fileService.getParentFile(target), conflicts, false, event.getOriginationId())) {
				return fileService.getFileByPath(target.getPath());
			} else {
				throw e;
			}
		}
	}
	
	public boolean processFileDelete(NimbusUser user, String path, String originationId) throws IOException {
		final SyncFile syncFile = toSyncFile(user, path);
		if (syncFile == null) return false;
		final NimbusFile nimbusFile = toNimbusFile(user, path);
		if (nimbusFile == null || !fileService.fileExistsOnDisk(nimbusFile)) return false;
		if (!fileService.delete(nimbusFile, originationId)) {
			throw new IOException("Error deleting file");
		}
		return true;
	}
	
	public void processFileUpload(NimbusUser user, String path, String originationId, InputStream in) throws IOException {
		NimbusFile file = toNimbusFile(user, path);
		NimbusFile parent = fileService.getParentFile(file);
		if (!fileService.fileExistsOnDisk(parent)) {
			throw new IOException("Upload parent directory does not exist: " + parent.getPath());
		}
		if (file.isDirectory()) {
			throw new IllegalArgumentException("Cannot request process a directory upload: " + file.getPath());
		}
		if (!isTrackedFile(user, file)) {
			throw new IllegalArgumentException("The file upload destination is not in the user's sync directory: " + file.getPath());
		}
		
		try (OutputStream out = new FileOutputStream(new File(file.getPath()))) {
			byte[] buffer = new byte[2048];
			int length = 0;
			while ((length = in.read(buffer)) != -1) {
				out.write(buffer, 0, length);
			}
			out.flush();
		} catch (IOException e) {
			throw e;
		}
		log.debug("Pre-reconcile file is " + file);
		fileService.reconcile(file, null, false, originationId); // update info
		log.debug("Post-reconcile file is " + file);
		hashIfNeeded(fileService.getFileByPath(file.getPath())); // Update hash. TODO: fix the ID setting after reconciliation...
	}
	
	// Returns true if the hash was updated AND changed
	private boolean hashIfNeeded(NimbusFile file) throws IOException {
		if (file.isDirectory()) return false;
		final String lastMd5 = file.getMd5();
		final Long modified = FileUtil.getLastModifiedTime(file);
		final Long size = FileUtil.getFileSize(file);
		//log.debug("Evaluating hash of " + file.getPath() + ". Size: " + file.getSize() + " vs. " + size + ". Last Hashed: " + lastHashed + " vs. modified " + modified 
		//		+ " (" + ComparatorUtil.nullSafeLongComparator(file.getSize(), size) + ", " + ComparatorUtil.nullSafeDateComparator(lastHashed, modified) + ")");
		if (lastMd5 == null || lastMd5.isEmpty() // We've never hashed it
				|| ComparatorUtil.nullSafeLongComparator(file.getSize(), size) != 0 // File size changed
				|| ComparatorUtil.nullSafeLongComparator(file.getLastModified(), modified) != 0 // Modified date has changed
				|| ComparatorUtil.nullSafeLongComparator(file.getLastHashed(), modified) < 0) {  // Haven't hashed since modification
			log.info("Calculating MD5 hash of " + file);
			file.setMd5(StringUtil.bytesToHex(HashUtil.hash(file)));
			file.setSize(size);
			file.setLastHashed(System.currentTimeMillis());
			file.setLastModified(modified);
			NimbusFileDAO.updateMd5(file);
			//log.debug("MD5 Updated for " + file.getPath() + ": " + oldMd5 + " vs. " + file.getMd5() + " (" + ComparatorUtil.nullSafeStringComparator(file.getMd5(), oldMd5) + ")");
		}
		return ComparatorUtil.nullSafeStringComparator(file.getMd5(), lastMd5) != 0;
	}
	
	private NimbusUser getFileUser(NimbusFile nf) {
		return userService.getUserById(nf.getUserId());
	}
	
	public boolean isTrackedFile(NimbusUser user, NimbusFile file) {
		final NimbusFile syncRoot = userService.getSyncRootFolder(user);
		return fileService.fileIsChildOf(file, syncRoot);
	}
	
	public SyncFile toSyncFile(NimbusUser user, String path) throws IOException {
		return toSyncFile(user, toNimbusFile(user, path));
	}
	
	public SyncFile toSyncFile(NimbusFile file) throws IOException {
		return toSyncFile(getFileUser(file), file);
	}
	
	public SyncFile toSyncFile(NimbusUser user, NimbusFile nf) throws IOException {
		final NimbusFile syncRoot = userService.getSyncRootFolder(user);
		if (!fileService.fileIsChildOf(nf, syncRoot)) return null;
		return SyncFileUtil.toSyncFile(syncRoot, nf);
	}
	
	public NimbusFile toNimbusFile(NimbusUser user, String path) {
		final NimbusFile syncRoot = userService.getSyncRootFolder(user);
		NimbusFile result = fileService.resolveRelativePath(syncRoot, path);
		if (result == null) {
			// File doesn't currently exist on the server
			result = fileService.getFileByPath(syncRoot.getPath() + path);
		}
		return result;
	}
	
	public NimbusFile toNimbusFile(NimbusUser user, SyncFile file) {
		final NimbusFile syncRoot = userService.getSyncRootFolder(user);
		NimbusFile result = fileService.resolveRelativePath(syncRoot, file.getPath());
		if (result == null) {
			// File doesn't currently exist on the server
			result = fileService.getFileByPath(syncRoot.getPath() + file.getPath());
		}
		return result;
	}
}
