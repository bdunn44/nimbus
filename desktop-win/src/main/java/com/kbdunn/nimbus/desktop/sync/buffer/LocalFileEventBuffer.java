package com.kbdunn.nimbus.desktop.sync.buffer;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.interfaces.FileManager;
import com.kbdunn.nimbus.common.sync.model.SyncFile;

public class LocalFileEventBuffer {

	private static final Logger log = LoggerFactory.getLogger(LocalFileEventBuffer.class);
	private static final long MAX_REMOTE_STATE_WAIT_MS = TimeUnit.SECONDS.toMillis(10);
	
	private List<SyncFile> uploadBuffer;
	private List<SyncFile> deleteBuffer;
	private ExecutorService executor;
	private List<SyncFile> remoteFiles;
	private CountDownLatch bufferReadyLatch;
	
	public LocalFileEventBuffer() {
		uploadBuffer = new LinkedList<>();
		deleteBuffer = new LinkedList<>();
		executor = Executors.newSingleThreadExecutor();
		bufferReadyLatch = new CountDownLatch(1);
	}
	
	public List<SyncFile> getUploadFileBuffer() {
		return uploadBuffer;
	}
	
	public List<SyncFile> getDeleteFileBuffer() {
		return deleteBuffer;
	}
	
	public List<SyncFile> getRemoteFiles() {
		return remoteFiles;
	}
	
	public void addFileToUpload(SyncFile file) {
		deleteBuffer.remove(file); // Remove from delete buffer if it exists
		uploadBuffer.add(file);
	}
	
	public void addFileToDelete(SyncFile file) {
		// Remove from upload buffer if it exists
		uploadBuffer.remove(file); 
		// Remove children as well
		if (file.isDirectory()) {
			uploadBuffer.removeIf((ufile) -> {
				return ufile.getPath().startsWith(file.getPath());
			});
		}
		deleteBuffer.add(file);
	}
	
	private void setReady() {
		bufferReadyLatch.countDown();
	}
	
	public void awaitReady() {
		if (remoteFiles == null) {
			try {
				if (!bufferReadyLatch.await(MAX_REMOTE_STATE_WAIT_MS, TimeUnit.MILLISECONDS)) {
					log.warn("Could not wait for remote sync state.");
				}
			} catch (InterruptedException e) {
				log.error("Remote sync state download interrupted.");
			}
		}
	}
	
	public void startFileListProcess(FileManager fileManager) {
		Future<List<SyncFile>> future = executor.submit(fileManager.createFileListProcess());
		try {
			remoteFiles = future.get();
			setReady();
		} catch (InterruptedException|ExecutionException e) {
			log.error("Error retrieving remote sync state!", e);
		} 
	}
}
