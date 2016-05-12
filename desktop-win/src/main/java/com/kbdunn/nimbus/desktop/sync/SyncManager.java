package com.kbdunn.nimbus.desktop.sync;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.NimbusWAsyncConnection;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;
import com.kbdunn.nimbus.desktop.sync.listener.LocalFileEventListener;
import com.kbdunn.nimbus.desktop.sync.listener.RemoteFileEventListener;
import com.kbdunn.nimbus.desktop.sync.process.BatchSynchronizeProcess;

public class SyncManager {

	public enum Status {
		DISCONNECTED("Disconnected", false),
		CONNECTION_ERROR("Connection Error", false),
		CONNECTED("Connected", true),
		SYNCING("Syncing Files...", true),
		SYNCED("Files Synced", true),
		SYNC_ERROR("Sync Error", true),
		PAUSED("Paused", true);
		
		private String desc;
		private boolean isConnected;
		
		private Status(String desc, boolean isConnected) {
			this.desc = desc;
			this.isConnected = isConnected;
		}
		
		public boolean isConnected() {
			return isConnected;
		}
		
		@Override
		public String toString() {
			return desc;
		}
		
		public static Status fromString(String status) {
			for (Status ss : values()) {
				if (ss.toString().equals(status)) return ss;
			}
			return null;
		}
	}
	
	private static final Logger log = LoggerFactory.getLogger(SyncManager.class);
	private static final int WORKER_THREAD_COUNT = 4;
	
	private Status status;
	private NimbusWAsyncConnection connection;
	private SyncEventHandler syncEventHandler;
	private NimbusFileObserver fileObserver;
	private ExecutorService executor;
	
	public SyncManager() {
		status = Status.DISCONNECTED;
		connection = new NimbusWAsyncConnection();
	}
	
	public Status getSyncStatus() {
		return status;
	}
	
	public ExecutorService getExecutor() {
		return executor;
	}
	
	public boolean connect() {
		final SyncCredentials creds = SyncPreferences.getCredentials();
		if (creds.equals(SyncCredentials.empty())) {
			// Don't even try
			status = Status.DISCONNECTED;
		} else {
			log.info("Connecting to Nimbus...");
			try {
				// Authenticate
				if (!connection.authenticate(creds)) {
					throw new Exception("Error authenticating " + creds.getUsername());
				}
				status = Status.CONNECTED;
				log.info("Connected");
				
				// Subscribe to network change events
				syncEventHandler = new SyncEventHandler(connection.getFileManager(), this);
				
				// Listen to local change events (not started until resume())
				fileObserver = new NimbusFileObserver(Application.getSyncRootDirectory());
				fileObserver.addFileObserverListener(new LocalFileEventListener(syncEventHandler));
			} catch (Exception e) {
				status = Status.CONNECTION_ERROR;
				log.error("Unable to connect to Nimbus", e);
			} 
		}
		Application.updateSyncStatus();
		
		return status.isConnected();
	}
	
	public void disconnect() {
		// Check that we're connected, if so, disconnect
		if (connection.isConnected()) {
			log.info("Disconnecting from Nimbus...");
			if (connection.disconnect()) { 
				log.info("Disconnected");
				status = Status.DISCONNECTED;
			} else {
				log.warn("Unable to disconnect");
			}
		} else {
			log.info("Already disconnected");
			status = Status.DISCONNECTED;
		}
		Application.updateSyncStatus();
	}
	
	public boolean isSyncActive() {
		return status.isConnected() && status != Status.PAUSED;
	}
	
	public void pause() {
		if (!status.isConnected()) {
			return;
		}
		log.info("Pausing file synchronization");
		// Shutdown executor
		try {
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.warn("Sync tasks interrupted");
		} finally {
			if (!executor.isTerminated()) {
				log.warn("Sync tasks cancelled");
			}
			executor.shutdownNow();
		}
		// Stop observing file events
		try {
			fileObserver.stop();
		} catch (Exception e) {
			log.error("Unable to stop file observer", e);
		}
		// Save current file state
		try {
			BatchFileSyncArbiter.persistCurrentSyncState();
		} catch (IOException e) {
			log.error("Unable to persist current sync state!", e);
		}
		
		status = Status.PAUSED;
		Application.updateSyncStatus();
	}
	
	public void resume() {
		if (!status.isConnected() || status == Status.SYNCING) {
			return;
		}
		log.info("Resuming file synchronization");
		// Initialize executor
		if (executor == null || executor.isTerminated()) {
			executor = Executors.newFixedThreadPool(WORKER_THREAD_COUNT);
		}
		// Run batch synchronize in background thread
		Application.getDisplay().asyncExec(() -> {
			status = Status.SYNCING;
			Application.updateSyncStatus();
			
			if (!batchSynchronize()) {
				log.error("Failed to batch synchronize!");
				status = Status.SYNC_ERROR;
				Application.updateSyncStatus();
				return;
			}  
			// If status is still "Syncing", update
			if (status == Status.SYNCING) {
				status = Status.SYNCED;
				Application.updateSyncStatus();
			}
			// TODO: Subscribe immediately, queue events if batch is still running?
			connection.getFileManager().subscribeFileEvents(new RemoteFileEventListener(syncEventHandler));
			try {
				fileObserver.start();
			} catch (Exception e) {
				log.error("Unable to start file observer", e);
			}
		});
	}
	
	private synchronized boolean batchSynchronize() {
		List<SyncFile> rootNode = null;
		try {
			long start = System.nanoTime();
			// Get network state synchronously
			rootNode = executor.submit(connection.getFileManager().createFileListProcess()).get();
			long end = System.nanoTime();
			log.debug("Took {}ms to fetch network file list.", TimeUnit.NANOSECONDS.toMillis(end-start));
		} catch (Exception e) {
			log.error("Error fetching network file list.", e);
			return false;
		}
		if (rootNode == null) {
			log.error("Error fetching network file list.");
			return false;
		} 
		
		try {
			long start = System.nanoTime();
			BatchFileSyncArbiter arbiter = new BatchFileSyncArbiter(rootNode);
			BatchSynchronizeProcess batchSync = new BatchSynchronizeProcess(arbiter, syncEventHandler);
			boolean success = batchSync.start();
			long end = System.nanoTime();
			log.debug("Took {}ms to process batch synchronization.", TimeUnit.NANOSECONDS.toMillis(end-start));
			if (success) {
				// Batch sync succeeded
				batchSync.persistSyncState();
				return true;
			}
		} catch (Exception e) {
			log.error("Error executing batch synchronization.", e);
		}
		return false;
	}
}
