package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class SyncEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);
	private static final long BUFFER_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
	
	private final RemoteFileManager fileManager;
	private final DesktopSyncManager syncManager;
	
	// Buffer to capture local change events
	private LocalFileEventBuffer currentBuffer;
	// Removing this for now.. seems to be asking for bugs
	//private final ExpectedEventFilter localEventFilter;
	
	public SyncEventHandler(RemoteFileManager fileManager, DesktopSyncManager syncManager) {
		this.fileManager = fileManager;
		this.syncManager = syncManager;
		//this.localEventFilter = new ExpectedEventFilter();
	}
	
	public void handleLocalFileAdd(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		/*if (localEventFilter.filterAdd(file)) {
			log.debug("Filtered expected local file add event for {}", file);
			return;
		}*/
		logChangeEvent(file, "created locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void processLocalFileAddOrUpdate(SyncFile file) {
		syncManager.getExecutor().submit(fileManager.createUploadProcess(file));
	}
	
	public void processRemoteFileAdd(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, "created remotely");
		try {
			SyncFile cachedFile = SyncStateCache.instance().get(syncFile);
			File file = DesktopSyncFileUtil.toFile(syncFile);
			if (syncFile.isDirectory()) {
				if (!file.exists()) {
					// Create the directory locally
					//localEventFilter.expectAdd(syncFile);
					FileUtils.forceMkdir(file);
					SyncStateCache.instance().update(syncFile);
					log.info("Folder created locally {}", syncFile);
				} else {
					log.info("Folder already exists locally {}", syncFile);
				}
			} else {
				if (cachedFile == null || !cachedFile.getMd5().equals(syncFile.getMd5())) {
					//localEventFilter.expectAdd(syncFile);
					syncManager.getExecutor().submit(fileManager.createDownloadProcess(syncFile));
					log.info("Download process submitted for {}", syncFile);
				} else {
					log.info("Updated file already exists locally. Skipping download of {}", syncFile);
				}
			}
		} catch (Exception e) {
			//localEventFilter.filterAdd(syncFile);
			log.error("Cannot download the remotely added file {}", syncFile);
		}
	}
	
	public void handleLocalFileUpdate(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		if (file.isDirectory()) return;
		/*if (localEventFilter.filterUpdate(file)
				|| (localEventFilter.filterDelete(file) && localEventFilter.filterAdd(file))) {
			log.debug("Filtered expected local file update event for {}", file);
			return;
		}*/
		logChangeEvent(file, "modified locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void processRemoteFileUpdate(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, "modified remotely");
		SyncFile cachedFile = SyncStateCache.instance().get(syncFile);
		try {
			if (cachedFile == null || !cachedFile.getMd5().equals(syncFile.getMd5())) {
				//localEventFilter.expectUpdate(cachedFile);
				syncManager.getExecutor().submit(fileManager.createDownloadProcess(syncFile));
				log.info("Download process submitted for {}", syncFile);
			} else {
				log.info("Updated file already exists locally. Skipping download of {}", syncFile);
			}
		} catch (Exception e) {
			//localEventFilter.filterUpdate(cachedFile);
			log.error("Cannot download the updated file {}", syncFile);
		}
	}
	
	public void handleLocalFileDelete(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		/*if (localEventFilter.filterDelete(file)) {
			log.debug("Filtered expected local file delete event for {}", file);
			return;
		}*/
		logChangeEvent(file, "deleted locally");
		checkBuffer();
		currentBuffer.addFileToDelete(file);
	}
	
	public void processLocalFileDelete(SyncFile file) {
		syncManager.getExecutor().submit(fileManager.createDeleteProcess(file));
	}
	
	public void processRemoteFileDelete(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, "deleted remotely");
		File file = DesktopSyncFileUtil.toFile(syncFile);
		if (!file.exists()) {
			log.info("File does not exist locally. Skipping delete of {}", syncFile);
			return;
		} 
		
		//localEventFilter.expectDelete(syncFile);
		if (FileUtils.deleteQuietly(file)) {
			SyncStateCache.instance().delete(syncFile);
			log.info("Deleted file " + syncFile);
		} else {
			//localEventFilter.filterDelete(syncFile);
			log.error("Could not delete file {}", syncFile);
		}
	}
	
	public void processRemoteFileMove(FileMoveEvent event) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(event.getSrcFile(), "moved remotely");
		try {
			final File srcFile = DesktopSyncFileUtil.toFile(event.getSrcFile());
			final File destFile = DesktopSyncFileUtil.toFile(event.getDstFile());
			if (!srcFile.exists()) {
				log.warn("Cannot process move, local file {} does not exist", event.getSrcFile());
				return;
			} else if (destFile.exists()) {
				if (event.getSrcFile().isDirectory()) {
					// TODO: This will cause issues if we're doing a directory move
					// ... which we should be at this point
					//localEventFilter.expectDelete(event.getSrcFile());
					FileUtils.deleteQuietly(srcFile);
					SyncStateCache.instance().delete(event.getSrcFile());
					log.info("Moved folder already exists. Deleted source {}", event.getSrcFile());
					return;
				} else if (!event.isReplaceExistingFile()) {
					// Don't replace files unless they were replaced remotely. Ignore folders.
					log.warn("Will not replace local file that was not replaced remotely {}", event.getDstFile());
					return;
				} else {
					//localEventFilter.expectDelete(event.getDstFile());
					FileUtils.deleteQuietly(destFile);
					SyncStateCache.instance().delete(event.getDstFile());
					log.info("Move destination exists, deleted file {}", event.getDstFile());
				}
			}
			
			if (srcFile.isFile()) {
				//localEventFilter.expectDelete(event.getSrcFile());
				//localEventFilter.expectAdd(event.getDstFile());
				FileUtils.moveFile(srcFile, destFile);
				SyncStateCache.instance().delete(event.getSrcFile());
				SyncStateCache.instance().update(event.getDstFile());
			} else {
				log.warn("Processing folder move. This shouldn't happen, move events are pushed individually.");
				FileUtils.moveDirectory(srcFile, destFile);
				SyncStateCache.instance().visit(event.getSrcFile());
				SyncStateCache.instance().visit(event.getDstFile());
			}
			log.info("Moved {} to {}", event.getSrcFile(), event.getDstFile());
		} catch (IOException e) {
			log.error("Error moving {} to {}", event.getSrcFile(), event.getDstFile(), e);
		}
	}
	
	public void processLocalFileCopy(FileCopyEvent event) {
		syncManager.getExecutor().submit(fileManager.createCopyProcess(event));
	}
	
	public void processRemoteFileCopy(FileCopyEvent event) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(event.getSrcFile(), "copied remotely");
		try {
			final SyncFile cachedSrc = SyncStateCache.instance().get(event.getSrcFile());
			final SyncFile cachedDst = SyncStateCache.instance().get(event.getDstFile());
			final File srcFile = DesktopSyncFileUtil.toFile(event.getSrcFile());
			final File destFile = DesktopSyncFileUtil.toFile(event.getDstFile());
			if (!srcFile.exists()) {
				log.warn("Cannot process copy, local file {} does not exist", event.getSrcFile());
				return;
			}
			if (destFile.exists()) {
				if (event.getDstFile().isDirectory()) {
					// Ignore, we're good
					return;
				} else if (!event.isReplaceExistingFile()) {
					log.warn("Will not replace local file that was not replaced remotely {}", event.getDstFile());
					return;
				} else if (cachedSrc.getMd5().equals(cachedDst.getMd5())) {
					log.info("Skipping local file copy, target file is already up-to-date {}", event.getDstFile());
					return;
				} else {
					//localEventFilter.expectDelete(event.getDstFile());
					FileUtils.deleteQuietly(destFile);
					SyncStateCache.instance().delete(event.getDstFile());
					log.info("Copy destination exists, deleted file {}", event.getDstFile());
				}
			}

			//localEventFilter.expectAdd(event.getDstFile());
			if (srcFile.isFile()) {
				FileUtils.copyFile(srcFile, destFile);
			} else {
				log.warn("Processing folder copy. This shouldn't happen, copy events are pushed individually.");
				FileUtils.copyDirectory(srcFile, destFile);
			}
			SyncStateCache.instance().update(event.getDstFile());
			log.info("Copied {} to {}", event.getSrcFile(), event.getDstFile());
		} catch (IOException e) {
			log.error("Error copying {} to {}", event.getSrcFile(), event.getDstFile(), e);
		}
	}
	
	public void processRemoteVersionConfict(SyncFile file) {
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
	
	private synchronized void checkBuffer() {
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
			buffer.startFileListProcess(fileManager);
			buffer.awaitReady();
			processBuffer(buffer);
		}, BUFFER_WAIT_TIME_MS, TimeUnit.MILLISECONDS);
	}
	
	private void processBuffer(final LocalFileEventBuffer buffer) {
		final List<SyncFile> toUpload = buffer.getUploadFileBuffer();
		filterUploadBuffer(toUpload, buffer.getRemoteFiles());
		final List<FileCopyEvent> toCopy = findBufferedCopyOpportunities(toUpload, buffer.getRemoteFiles());
		filterCopiesFromUploadBuffer(toUpload, toCopy);
		final List<SyncFile> toDelete = buffer.getDeleteFileBuffer();
		filterDeleteBuffer(toDelete, buffer.getRemoteFiles());
		final List<SyncFile> newFolders = new ArrayList<>();
		
		// Detect folder adds to be processed first
		SyncFileUtil.sortPreorder(toUpload);
		for (SyncFile ufile : toUpload) {
			if (ufile.isDirectory()) {
				newFolders.add(ufile);
			}
		}
		
		log.debug("Filtered file event buffer contains {} folder add(s), {} upload(s), {} delete(s), {} copy(ies).", 
				newFolders.size(), toUpload.size(), toDelete.size(), toCopy.size());
		try {
			long start = System.nanoTime();
			if (!newFolders.isEmpty()) {
				syncManager.getExecutor().submit(fileManager.createFolderAddProcess(newFolders)).get();
				log.debug("Processed folder adds in {}ms. Starting async processing.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
			}
			
			// Process the other upload, delete, and copy operations
			for (SyncFile ufile : toUpload) {
				if (!ufile.isDirectory()) {
					processLocalFileAddOrUpdate(ufile);
				}
			}
			for (FileCopyEvent copy : toCopy) {
				syncManager.getExecutor().submit(fileManager.createCopyProcess(copy));
			}
			SyncFileUtil.sortPreorder(toDelete);
			Collections.reverse(toDelete);
			for (SyncFile dfile : toDelete) {
				this.processLocalFileDelete(dfile);
			}
			log.debug("Done processing buffer in {}ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		} catch (InterruptedException|IllegalArgumentException|ExecutionException e) {
			log.error("Error processing local event buffer!", e);
		} 
	}
	
	private List<FileCopyEvent> findBufferedCopyOpportunities(List<SyncFile> toUpload, List<SyncFile> remoteFiles) {
		List<FileCopyEvent> copies = new ArrayList<>();
		for (SyncFile upload : toUpload) {
			if (upload.isDirectory()) continue;
			SyncFile remoteCopy = null;
			boolean replace = false;
			for (SyncFile remote : remoteFiles) {
				if (upload.getPath().equals(remote.getPath())) {
					replace = true;
				} else if (upload.getMd5().equals(remote.getMd5())) {
					remoteCopy = remote;
				}
				if (remoteCopy != null && replace) {
					break;
				}
			}
			if (remoteCopy != null) {
				FileCopyEvent event = new FileCopyEvent(remoteCopy, upload, replace);
				log.debug("Found copy opportunity {}", event);
				copies.add(event);
			}
		}
		return copies;
	}
	
	private void filterUploadBuffer(List<SyncFile> toUpload, List<SyncFile> remoteFiles) {
		// Remove the files from the buffer which are already on the server
		toUpload.removeIf((localFile) -> {
			// We've already hashed it if needed
			/*byte[] localMd5 = null;
			try {
				localMd5 = HashUtil.hash(new File(localFile.getPath()));
			} catch (IOException e) {
				log.error("Unable to hash file!", e);
			}*/
			for (SyncFile remoteFile : remoteFiles) {
				if (remoteFile.getPath().equals(localFile.getPath()) &&
						((localFile.isDirectory() && remoteFile.isDirectory())
								|| (localFile.getMd5() != null && remoteFile.getMd5().equals(localFile.getMd5())))) {
					// An up-to-date remote file already exists
					log.debug("Filtered upload that is already up-to-date on the server {}", localFile);
					return true;
				}
			}
			return false;
		});
	}
	
	private void filterCopiesFromUploadBuffer(List<SyncFile> toUpload, List<FileCopyEvent> copyEvents) {
		toUpload.removeIf((localFile) -> {
			for (FileCopyEvent copyEvent : copyEvents) {
				if (copyEvent.getDstFile().equals(localFile)) { 
					// We're going to copy it remotely instead
					log.debug("Filtered upload that is being copied instead {}", localFile);
					return true;
				}
			}
			return false;
		});
	}
	
	private void filterDeleteBuffer(List<SyncFile> toDelete, List<SyncFile> remoteFiles) {
		toDelete.removeIf((localFile) -> {
			// Remove children
			for (SyncFile possibleParent : toDelete) {
				if (!possibleParent.equals(localFile) 
						&& localFile.getPath().startsWith(possibleParent.getPath())) {
					log.debug("Filtered child delete {}", localFile);
					return true;
				}
			}
			// Remove files that don't exist remotely
			for (SyncFile remoteFile : remoteFiles) {
				if (remoteFile.getPath().equals(localFile.getPath())) {
					return false;
				}
			}
			log.debug("Filtered local delete that isn't on the network {}", localFile);
			return true;
		});
	}
}
