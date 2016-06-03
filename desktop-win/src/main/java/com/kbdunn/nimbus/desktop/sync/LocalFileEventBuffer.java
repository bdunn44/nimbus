package com.kbdunn.nimbus.desktop.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;

public class LocalFileEventBuffer {

	private static final Logger log = LoggerFactory.getLogger(LocalFileEventBuffer.class);
	private static final long MAX_REMOTE_STATE_WAIT_MS = TimeUnit.SECONDS.toMillis(30);
	
	private Map<String, SyncFile> uploadBuffer;
	private Map<String, SyncFile> deleteBuffer;
	private List<SyncFile> remoteFiles;
	private CountDownLatch bufferReadyLatch;
	
	public LocalFileEventBuffer() {
		uploadBuffer = new LinkedHashMap<>();
		deleteBuffer = new LinkedHashMap<>();
		bufferReadyLatch = new CountDownLatch(1);
	}
	
	public List<SyncFile> getUploadFileBuffer() {
		return new ArrayList<>(uploadBuffer.values());
	}
	
	public List<SyncFile> getDeleteFileBuffer() {
		return new ArrayList<>(deleteBuffer.values());
	}
	
	public List<SyncFile> getRemoteFiles() {
		return remoteFiles;
	}
	
	public void addFileToUpload(SyncFile file) {
		deleteBuffer.remove(file.getPath()); // Remove from delete buffer if it exists
		uploadBuffer.put(file.getPath(), file);
	}
	
	public void addFileToDelete(SyncFile file) {
		// Remove from upload buffer if it exists
		uploadBuffer.remove(file.getPath()); 
		// Remove children as well
		if (file.isDirectory()) {
			for (Iterator<Map.Entry<String, SyncFile>> it = uploadBuffer.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, SyncFile> entry = it.next();
				if (entry.getKey().startsWith(file.getPath())) {
					it.remove();
				}
			}
		}
		deleteBuffer.put(file.getPath(), file);
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
	
	public void startFileListProcess(RemoteFileManager fileManager) {
		Application.getSyncManager().getExecutor().submit(() -> {
			try {
				remoteFiles = fileManager.createFileListProcess().call();
				bufferReadyLatch.countDown();
			} catch (Exception e) {
				log.error("Error retrieving network sync file state", e);
			}
		});
	}
}