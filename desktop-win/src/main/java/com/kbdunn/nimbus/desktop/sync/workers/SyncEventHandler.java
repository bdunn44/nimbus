package com.kbdunn.nimbus.desktop.sync.workers;

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
import com.kbdunn.nimbus.desktop.ApplicationProperties;
import com.kbdunn.nimbus.desktop.sync.SyncManager;
import com.kbdunn.nimbus.desktop.sync.RemoteFileManager;
import com.kbdunn.nimbus.desktop.sync.data.LocalFileEventBuffer;
import com.kbdunn.nimbus.desktop.sync.data.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.data.SyncStateCache;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

public class SyncEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);
	private static final long BUFFER_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
	
	private final RemoteFileManager fileManager;
	private final SyncManager syncManager;
	
	// Buffer to capture local change events
	private LocalFileEventBuffer currentBuffer;
	
	public SyncEventHandler(RemoteFileManager fileManager, SyncManager syncManager) {
		this.fileManager = fileManager;
		this.syncManager = syncManager;
	}
	
	public void handleLocalFileAdd(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, true, "add");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void processLocalFileAddOrUpdate(SyncFile file) {
		syncManager.getExecutor().submit(fileManager.createUploadProcess(file));
	}
	
	public void processRemoteFileAdd(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, false, "add");
		try {
			SyncFile cachedFile = SyncStateCache.instance().get(syncFile);
			File file = DesktopSyncFileUtil.toFile(syncFile);
			if (syncFile.isDirectory()) {
				if (!file.exists()) {
					// Create the directory locally
					FileUtils.forceMkdir(file);
					log.info("Created folder locally {}", syncFile);
				} else {
					log.info("Folder already exists locally {}", syncFile);
				}
				SyncStateCache.instance().update(syncFile);
			} else {
				if (cachedFile == null || !cachedFile.getMd5().equals(syncFile.getMd5())) {
					syncManager.getExecutor().submit(fileManager.createDownloadProcess(syncFile));
				} else {
					log.info("Updated file already exists locally. Skipping download of {}", syncFile);
				}
			}
		} catch (Exception e) {
			log.error("Could not download the remotely added file {}", syncFile, e);
		}
	}
	
	public void handleLocalFileUpdate(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		if (file.isDirectory()) return;
		logChangeEvent(file, true, "update");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void processRemoteFileUpdate(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, false, "update");
		SyncFile cachedFile = SyncStateCache.instance().get(syncFile);
		try {
			if (cachedFile == null || !cachedFile.getMd5().equals(syncFile.getMd5())) {
				syncManager.getExecutor().submit(fileManager.createDownloadProcess(syncFile));
			} else {
				log.info("Updated file already exists locally. Skipping download of {}", syncFile);
			}
		} catch (Exception e) {
			log.error("Could not download the updated file {}", syncFile, e);
		}
	}
	
	public void handleLocalFileDelete(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, true, "delete");
		checkBuffer();
		currentBuffer.addFileToDelete(file);
	}
	
	public void processLocalFileDelete(SyncFile file) {
		syncManager.getExecutor().submit(fileManager.createDeleteProcess(file));
	}
	
	public void processRemoteFileDelete(SyncFile syncFile) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(syncFile, false, "delete");
		File file = DesktopSyncFileUtil.toFile(syncFile);
		if (!file.exists()) {
			log.info("File does not exist locally. Skipping delete of {}", syncFile);
			SyncStateCache.instance().delete(syncFile); // Make sure it's deleted in the cache
			return;
		} 
		
		if (FileUtils.deleteQuietly(file)) {
			SyncStateCache.instance().delete(syncFile);
			log.info("Deleted file locally " + syncFile);
		} else {
			log.error("Could not delete file {}", syncFile);
		}
	}
	
	public void processRemoteFileMove(FileMoveEvent event) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(event.getSrcFile(), false, "move");
		try {
			final File srcFile = DesktopSyncFileUtil.toFile(event.getSrcFile());
			final File destFile = DesktopSyncFileUtil.toFile(event.getDstFile());
			final SyncFile cachedSrc = SyncStateCache.instance().get(event.getSrcFile());
			if (!srcFile.exists() || !cachedSrc.getMd5().equals(event.getSrcFile().getMd5())) {
				log.warn("Could not process move, local file {} does not exist or differs from the server. Downloading instead.", event.getSrcFile());
				processRemoteFileUpdate(event.getDstFile());
				return;
			} else if (destFile.exists()) {
				if (event.getSrcFile().isDirectory()) {
					FileUtils.deleteQuietly(srcFile);
					SyncStateCache.instance().delete(event.getSrcFile());
					log.info("Moved folder already exists. Deleted source {}", event.getSrcFile());
					return;
				} else if (!event.isReplaceExistingFile()) {
					// Don't replace files unless they were replaced remotely. Ignore folders.
					log.warn("Will not replace local file that was not replaced remotely {}", event.getDstFile());
					return;
				} else {
					FileUtils.deleteQuietly(destFile);
					SyncStateCache.instance().delete(event.getDstFile());
					log.info("Move destination exists, deleted file in preparation for move {}", event.getDstFile());
				}
			}
			
			if (srcFile.isFile()) {
				FileUtils.moveFile(srcFile, destFile);
				SyncStateCache.instance().delete(event.getSrcFile());
				SyncStateCache.instance().update(event.getDstFile());
			} else {
				log.warn("Processing folder move. This shouldn't happen, move events are pushed individually.");
				FileUtils.moveDirectory(srcFile, destFile);
				SyncStateCache.instance().visit(event.getSrcFile());
				SyncStateCache.instance().visit(event.getDstFile());
			}
			log.info("Moved local file from {} to {}", event.getSrcFile(), event.getDstFile());
		} catch (IOException e) {
			log.error("Error moving local file from {} to {}", event.getSrcFile(), event.getDstFile(), e);
		}
	}
	
	public void processLocalFileCopy(FileCopyEvent event) {
		syncManager.getExecutor().submit(fileManager.createCopyProcess(event));
	}
	
	public void processRemoteFileCopy(FileCopyEvent event) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(event.getSrcFile(), false, "copy");
		try {
			final SyncFile cachedSrc = SyncStateCache.instance().get(event.getSrcFile());
			final SyncFile cachedDst = SyncStateCache.instance().get(event.getDstFile());
			final File srcFile = DesktopSyncFileUtil.toFile(event.getSrcFile());
			final File destFile = DesktopSyncFileUtil.toFile(event.getDstFile());
			if (!srcFile.exists() || !cachedSrc.getMd5().equals(event.getSrcFile().getMd5())) {
				log.warn("Could not process copy, local file {} does not exist or differs from the server. Downloading instead.", event.getSrcFile());
				processRemoteFileUpdate(event.getDstFile());
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
					FileUtils.deleteQuietly(destFile);
					SyncStateCache.instance().delete(event.getDstFile());
					log.info("Copy destination exists, deleted file in preparation for copy {}", event.getDstFile());
				}
			}
			
			if (srcFile.isFile()) {
				FileUtils.copyFile(srcFile, destFile);
			} else {
				log.warn("Processing folder copy. This shouldn't happen, copy events are pushed individually.");
				FileUtils.copyDirectory(srcFile, destFile);
			}
			SyncStateCache.instance().update(event.getDstFile());
			log.info("Copied local file from {} to {}", event.getSrcFile(), event.getDstFile());
		} catch (IOException e) {
			log.error("Error copying local file from {} to {}", event.getSrcFile(), event.getDstFile(), e);
		}
	}
	
	public void processRemoteVersionConfict(SyncFile file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, false, "version conflict");
		try {
			// Rename the local file
			File local = new File(ApplicationProperties.instance().getSyncDirectory(), file.getPath());
			String localName = local.getName();
			String rename = localName.substring(0, localName.lastIndexOf(".")) 
					+ " - " + SyncPreferences.getNodeName()
					+ localName.substring(localName.lastIndexOf("."));
			if (!local.renameTo(new File(ApplicationProperties.instance().getSyncDirectory(), rename))) {
				log.warn("Unable to rename conflicted local file {}", local.getAbsolutePath());
				SyncStateCache.instance().addError(file);
				return;
			}
			log.info("Renamed conflicted local file {} to {}", local.getAbsolutePath(), rename);
			
			// Download the network file
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Could not process remote version conflict {}", file, e);
		}
	}
	
	private void logChangeEvent(SyncFile file, boolean local, String event) {
		log.info("Detected {} {} {}: {}",
				local ? "local" : "remote",
				file.isDirectory() ? "folder" : "file", 
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
		log.info("Start buffering local events for {} ms.", BUFFER_WAIT_TIME_MS);
		Application.asyncExec(() -> {
			currentBuffer = null;
			log.info("Finished buffering local events. {} file(s) added/modified, {} file(s) deleted.", 
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
		
		log.info("Filtered file event buffer contains {} folder add(s), {} upload(s), {} delete(s), {} copy(ies).", 
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
			log.info("Done processing buffer in {}ms.", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
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
