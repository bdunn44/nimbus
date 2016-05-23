package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;
import com.kbdunn.nimbus.desktop.sync.buffer.LocalFileEventBuffer;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class SyncEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);
	private static final long BUFFER_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
	
	private final RemoteFileManager fileManager;
	private final DesktopSyncManager syncManager;
	
	// Buffer to capture local change events
	private LocalFileEventBuffer currentBuffer;
	
	public SyncEventHandler(RemoteFileManager fileManager, DesktopSyncManager syncManager) {
		this.fileManager = fileManager;;
		this.syncManager = syncManager;
	}
	
	public void handleLocalFileAdd(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void handleRemoteFileAdd(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created remotely");
		try {
			if (file.isDirectory()) {
				// Create the directory locally
				FileUtils.forceMkdir(DesktopSyncFileUtil.toFile(file));
			} else {
				Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
				syncManager.getExecutor().submit(downloadProcess);
			}
		} catch (Exception e) {
			log.error("Cannot download the remotely added file {}", file);
		}
	}
	
	public void handleLocalFileUpdate(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		if (file.isDirectory()) return;
		logChangeEvent(file, "modified locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void handleRemoteFileUpdate(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "modified remotely");
		try {
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Cannot download the updated file {}", file);
		}
	}
	
	public void handleLocalFileDelete(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted locally");
		checkBuffer();
		currentBuffer.addFileToDelete(file);
	}
	
	public void handleRemoteFileDelete(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted remotely");
		if (new File(file.getPath()).delete()) {
			log.error("Deleted file " + file);
		} else {
			log.error("Could not delete file {}", file);
		}
	}
	
	public void handleRemoteFileMove(SyncFile src, SyncFile dest) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(src, "moved remotely");
		try {
			final File srcfile = new File(src.getPath());
			final File destFile = new File(dest.getPath());
			if (srcfile.isFile()) {
				FileUtils.moveFile(srcfile, destFile);
			} else {
				FileUtils.moveDirectory(srcfile, destFile);
			}
		} catch (IOException e) {
			log.error("Error moving {} {} to {}", src.isDirectory() ? "folder" : "file", src, dest);
		}
	}
	
	public void hadleRemoteFileCopy(SyncFile src, SyncFile dest) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(src, "copied remotely");
		try {
			final File srcfile = new File(src.getPath());
			final File destFile = new File(dest.getPath());
			if (srcfile.isFile()) {
				FileUtils.copyFile(srcfile, destFile);
			} else {
				FileUtils.copyDirectory(srcfile, destFile);
			}
		} catch (IOException e) {
			log.error("Error moving {} {} to {}", src.isDirectory() ? "folder" : "file", src, dest);
		}
	}
	
	public void handleRemoteVersionConfict(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "has a remote version conflict");
		try {
			// Rename the local file
			File local = new File(SyncPreferences.getSyncDirectory(), file.getPath());
			String localName = local.getName();
			String rename = localName.substring(0, localName.lastIndexOf(".")) 
					+ " - " + SyncPreferences.getNodeName()
					+ localName.substring(localName.lastIndexOf("."));
			log.info("Renaming local file {} to {}", local.getAbsolutePath(), rename);
			if (!local.renameTo(new File(SyncPreferences.getSyncDirectory(), rename))) {
				log.warn("Unable to rename file {}", local.getAbsolutePath());
				// TODO sync error icon on file
				return;
			}
			
			// Download the network file
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Cannot download the remote file {}", file);
		}
	}
	
	private void logChangeEvent(SyncFile file, String event) {
		log.info("{} {}: {}", 
				file.isDirectory() ? "Directory" : "File", 
				event, 
				file
			);
	}
	
	private void checkBuffer() {
		if (currentBuffer == null) {
			currentBuffer = new LocalFileEventBuffer();
			startBuffering(currentBuffer);
		}
	}
	
	private void startBuffering(final LocalFileEventBuffer buffer) {
		log.debug("Start buffering for {} ms.", BUFFER_WAIT_TIME_MS);
		Application.asyncExec(() -> {
			currentBuffer = null;
			log.debug("Finished buffering. {} file(s) added/modified, {} file(s) deleted.", 
					buffer.getUploadFileBuffer().size(),
					buffer.getDeleteFileBuffer().size());
			
			// Process the buffer in the sync task worker thread pool
			Application.getSyncManager().getExecutor().submit(() -> {
				buffer.awaitReady();
				processBuffer(buffer);
			});
		}, BUFFER_WAIT_TIME_MS, TimeUnit.MILLISECONDS);
		// Start retrieving network state while buffering
		currentBuffer.startFileListProcess(fileManager);
	}
	
	private void processBuffer(final LocalFileEventBuffer buffer) {
		final List<SyncFile> toUpload = filterUploadBuffer(buffer.getUploadFileBuffer(), buffer.getRemoteFiles());
		final List<SyncFile> toDelete = filterDeleteBuffer(buffer.getDeleteFileBuffer(), buffer.getRemoteFiles());
		final List<SyncFile> newFolders = new ArrayList<>();
		final List<Callable<Void>> asyncProcesses = new ArrayList<>();
		
		SyncFileUtil.sortPreorder(toUpload);
		for (SyncFile ufile : toUpload) {
			if (ufile.isDirectory()) {
				newFolders.add(ufile);
			} else {
				asyncProcesses.add(fileManager.createAddProcess(ufile));
			}
		}
		for (SyncFile dfile : toDelete) {
			asyncProcesses.add(fileManager.createDeleteProcess(dfile));
		}
		log.debug("Processing local event buffer. {} new folder(s), {} async add/delete process(es).", newFolders.size(), asyncProcesses.size());
		try {
			long start = System.nanoTime();
			// Process folder creates in order, synchronously
			syncManager.getExecutor().submit(fileManager.createFolderAddProcess(newFolders)).get();
			log.debug("Done processing folder adds in {}ms. Starting async processing.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
			syncManager.getExecutor().invokeAll(asyncProcesses);
			log.debug("Done processing buffer in {}ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		} catch (InterruptedException|IllegalArgumentException|ExecutionException e) {
			log.error("Error processing local event buffer!", e);
		} 
	}
	
	private List<SyncFile> filterUploadBuffer(List<SyncFile> toUpload, List<SyncFile> remoteFiles) {
		List<SyncFile> filtered = new ArrayList<>(toUpload);

		// Remove the files from the buffer which are already on the server
		filtered.removeIf((localFile) -> {
			byte[] localMd5 = null;
			try {
				localMd5 = HashUtil.hash(new File(localFile.getPath()));
			} catch (IOException e) {
				log.error("Unable to hash file!", e);
			}
			for (SyncFile remoteFile : remoteFiles) {
				if (remoteFile.getPath().equals(localFile.getPath()) &&
						((localFile.isDirectory() && remoteFile.isDirectory())
								|| (localMd5 != null && remoteFile.getMd5().equals(localMd5)))) {
					return true;
				}
			}
			return false;
		});
		
		return filtered;
	}
	
	private List<SyncFile> filterDeleteBuffer(List<SyncFile> toDelete, List<SyncFile> remoteFiles) {
		List<SyncFile> filtered = new ArrayList<>(toDelete);
		
		// Remove children
		filtered.removeIf((localFile) -> {
			for (SyncFile possibleParent : toDelete) {
				if (localFile.getPath().startsWith(possibleParent.getPath())) {
					return true;
				}
			}
			return false;
		});
		
		// Remove the files from the buffer which aren't on the server
		filtered.removeIf((localFile) -> {
			for (SyncFile remoteFile : remoteFiles) {
				if (remoteFile.getPath().equals(localFile.getPath())) {
					return false;
				}
			}
			return true;
		});
		
		return filtered;
	}
}
