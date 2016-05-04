package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.sync.interfaces.FileManager;
import com.kbdunn.nimbus.common.sync.model.FileNode;
import com.kbdunn.nimbus.common.util.FileUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.buffer.LocalFileEventBuffer;

public class SyncEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);
	private static final long BUFFER_WAIT_TIME_MS = TimeUnit.SECONDS.toMillis(5);
	
	private final FileManager fileManager;
	private final SyncManager syncManager;
	
	// Buffer to capture local change events
	private LocalFileEventBuffer currentBuffer;
	private ExecutorService bufferProcessorExecutor;
	
	public SyncEventHandler(FileManager fileManager, SyncManager syncManager) {
		this.fileManager = fileManager;
		this.syncManager = syncManager;
		bufferProcessorExecutor = Executors.newSingleThreadExecutor();
	}
	
	public void handleLocalFileAdd(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void handleRemoteFileAdd(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created remotely");
		try {
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Cannot download the new file {}", file.getAbsolutePath());
		}
	}
	
	public void handleLocalFileUpdate(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "modified locally");
		checkBuffer();
		currentBuffer.addFileToUpload(file);
	}
	
	public void handleRemoteFileUpdate(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "modified remotely");
		try {
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Cannot download the updated file {}", file.getAbsolutePath());
		}
	}
	
	public void handleLocalFileDelete(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted locally");
		checkBuffer();
		currentBuffer.addFileToDelete(file);
	}
	
	public void handleRemoteFileDelete(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted remotely");
		if (file.delete()) {
			log.error("Deleted file " + file);
		} else {
			log.error("Could not delete file {}", file.getAbsolutePath());
		}
	}
	
	public void handleRemoteFileMove(File src, File dest) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(src, "moved remotely");
		try {
			if (src.isFile()) {
				FileUtils.moveFile(src, dest);
			} else {
				FileUtils.moveDirectory(src, dest);
			}
		} catch (IOException e) {
			log.error("Error moving {} {} to {}", src.isDirectory() ? "folder" : "file", src.getAbsolutePath(), dest.getAbsolutePath());
		}
	}
	
	public void handleRemoteVersionConfict(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "has a remote version conflict");
		try {
			// Rename the local file
			File local = new File(Application.getSyncRootDirectory(), file.getPath());
			String rename = local.getName() + "-" + SyncPreferences.getNodeName();
			log.info("Renaming local file {} to {}", local.getAbsolutePath(), rename);
			if (!local.renameTo(new File(Application.getSyncRootDirectory(), rename))) {
				log.warn("Unable to rename file {}", local.getAbsolutePath());
				// TODO sync error icon on file
				return;
			}
			
			// Download the network file
			Callable<Void> downloadProcess = fileManager.createDownloadProcess(file);
			syncManager.getExecutor().submit(downloadProcess);
		} catch (Exception e) {
			log.error("Cannot download the remote file {}", file.getAbsolutePath());
		}
	}
	
	private void logChangeEvent(File file, String event) {
		log.info("{} {}: {}", file.isDirectory() ? "Directory" : "File", event, file.getAbsolutePath());
	}
	
	private void checkBuffer() {
		if (currentBuffer == null) {
			currentBuffer = new LocalFileEventBuffer();
			startBuffering(currentBuffer);
		}
	}
	
	private void startBuffering(final LocalFileEventBuffer buffer) {
		log.debug("Start buffering for {} ms.", BUFFER_WAIT_TIME_MS);
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				currentBuffer = null;
				log.debug("Finished buffering. {} file(s) added/modified, {} file(s) deleted.", 
						buffer.getUploadFileBuffer().size(),
						buffer.getDeleteFileBuffer().size());
				
				// Queue the buffer to be processed sequentially
				bufferProcessorExecutor.submit(() -> {
					buffer.awaitReady();
					processBuffer(buffer);
				});
			}
		}, BUFFER_WAIT_TIME_MS);
		// Start retrieving network state while buffering
		currentBuffer.startFileListProcess(fileManager);
	}
	
	private void processBuffer(final LocalFileEventBuffer buffer) {
		final List<File> toUpload = filterUploadBuffer(buffer.getUploadFileBuffer(), buffer.getRemoteFiles());
		final List<File> toDelete = filterDeleteBuffer(buffer.getDeleteFileBuffer(), buffer.getRemoteFiles());
		final List<File> newFolders = new ArrayList<>();
		final List<Callable<Void>> asyncProcesses = new ArrayList<>();
		
		FileUtil.sortPreorder(toUpload);
		for (File ufile : toUpload) {
			if (ufile.isDirectory()) {
				newFolders.add(ufile);
			} else {
				asyncProcesses.add(fileManager.createAddProcess(ufile));
			}
		}
		for (File dfile : toDelete) {
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
	
	private List<File> filterUploadBuffer(List<File> toUpload, List<FileNode> remoteFiles) {
		List<File> filtered = new ArrayList<>(toUpload);

		// Remove the files from the buffer which are already on the server
		filtered.removeIf((localFile) -> {
			byte[] localMd5 = null;
			try {
				localMd5 = HashUtil.hash(localFile);
			} catch (IOException e) {
				log.error("Unable to hash file!", e);
			}
			for (FileNode remoteFile : remoteFiles) {
				if (remoteFile.getFile().equals(localFile) &&
						((localFile.isDirectory() && remoteFile.isFolder())
								|| (localMd5 != null && HashUtil.compare(remoteFile.getMd5(), localMd5)))) {
					return true;
				}
			}
			return false;
		});
		
		return filtered;
	}
	
	private List<File> filterDeleteBuffer(List<File> toDelete, List<FileNode> remoteFiles) {
		List<File> filtered = new ArrayList<>(toDelete);
		
		// Remove children
		filtered.removeIf((localFile) -> {
			for (File possibleParent : toDelete) {
				if (localFile.getPath().startsWith(possibleParent.getPath())) {
					return true;
				}
			}
			return false;
		});
		
		// Remove the files from the buffer which aren't on the server
		filtered.removeIf((localFile) -> {
			for (FileNode remoteFile : remoteFiles) {
				if (remoteFile.getFile().equals(localFile)) {
					return false;
				}
			}
			return true;
		});
		
		return filtered;
	}
}
