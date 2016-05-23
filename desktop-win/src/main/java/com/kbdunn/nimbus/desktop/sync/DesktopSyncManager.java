package com.kbdunn.nimbus.desktop.sync;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.NimbusClient;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;
import com.kbdunn.nimbus.desktop.sync.listener.LocalFileEventListener;
import com.kbdunn.nimbus.desktop.sync.process.BatchSynchronizeProcess;

public class DesktopSyncManager {

	public enum Status {
		DISCONNECTED("Disconnected", false),
		CONNECTING("Connecting...", false),
		CONNECTED("Connected", true),
		CONNECTION_ERROR("Connection Error", false),
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
	
	private static final Logger log = LoggerFactory.getLogger(DesktopSyncManager.class);
	private static final int WORKER_THREAD_COUNT = 1;
	
	private Status status;
	private NimbusClient client;
	private RemoteFileManager fileManager;
	private SyncEventHandler syncEventHandler;
	private NimbusFileObserver fileObserver;
	private ExecutorService executor;
	
	public DesktopSyncManager() {
		status = Status.DISCONNECTED;
	}
	
	public Status getSyncStatus() {
		return status;
	}
	
	public ExecutorService getExecutor() {
		return executor;
	}
	
	public boolean connect() {
		final SyncCredentials creds = SyncPreferences.getCredentials();
		final String url = SyncPreferences.getEndpoint();
		if (creds.equals(SyncCredentials.empty()) || url == null || url.isEmpty()) {
			// Don't even try
			status = Status.DISCONNECTED;
		} else {
			log.info("Connecting to Nimbus at {}...", url);
			status = Status.CONNECTING;
			Application.updateSyncStatus();
			try {
				// Authenticate
				if (client != null) {
					client.disconnect();
				}
				client = new NimbusClient(url, creds.toNimbusApiCredentials(), NimbusClient.Type.HTTP);
				// Check REST API authentication
				if (!client.authenticate()) {
					throw new TransportException("Error authenticating " + creds.getUsername());
				}
				// Connect to PUSH. Don't listen to events until resume()
				if (!client.connectToPushService()) {
					throw new TransportException("Error connecting to push service");
				}
				status = Status.CONNECTED;
				log.info("Connected");
				
				// Subscribe to network change events
				fileManager = new RemoteFileManager(client);
				syncEventHandler = new SyncEventHandler(fileManager, this);
				
				// Listen to local change events (not started until resume())
				fileObserver = new NimbusFileObserver(SyncPreferences.getSyncDirectory());
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
		log.info("Disconnecting from Nimbus...");
		client.disconnect();
		log.info("Disconnected");
		status = Status.DISCONNECTED;
		Application.updateSyncStatus();
	}
	
	public boolean isSyncActive() {
		return status.isConnected() 
				&& status != Status.PAUSED
				&& status != Status.CONNECTED // CONNECTED is not actively syncing
			;
	}
	
	public void pause() {
		if (!status.isConnected()) {
			return;
		}
		log.info("Pausing file synchronization");
		
		// Unsubscribe from push events
		fileManager.unsubscribeAll();
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
			SyncStateCache.instance().persist();
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
		Application.asyncExec(() -> {
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
			// TODO: Subscribe immediately, queue events while batch is still running
			/*try {
				if (!client.isConnectedToPushService()) {
					log.warn("Client is no longer connected to the push service. Reconnecting...");
					if (!client.connectToPushService()) {
						throw new TransportException("Error connecting to push service");
					}
				}
				// Subscribe to pushed events
				fileManager.subscribeFileEvents(new RemoteFileEventListener(syncEventHandler));
			} catch (Exception e) {
				log.error("Error encountered while subscribing to pushed events", e);
				pause();
				disconnect();
			}
			try {
				fileObserver.start();
			} catch (Exception e) {
				log.error("Unable to start file observer", e);
				pause();
				disconnect();
			}*/
		});
	}
	
	private synchronized boolean batchSynchronize() {
		List<SyncFile> networkState = null;
		try {
			long start = System.nanoTime();
			// Get network state synchronously
			networkState = executor.submit(fileManager.createFileListProcess()).get();
			long end = System.nanoTime();
			log.debug("Took {}ms to fetch network file list.", TimeUnit.NANOSECONDS.toMillis(end-start));
		} catch (Exception e) {
			log.error("Error fetching network file list.", e);
			return false;
		}
		if (networkState == null) {
			log.error("Error fetching network file list");
			return false;
		} 
		
		try {
			long start = System.nanoTime();
			SyncStateCache.instance().awaitCacheReady();
			BatchFileSyncArbiter arbiter = new BatchFileSyncArbiter(networkState);
			BatchSynchronizeProcess batchSync = new BatchSynchronizeProcess(arbiter, syncEventHandler);
			boolean success = batchSync.start();
			long end = System.nanoTime();
			if (success) {
				// Batch sync succeeded
				SyncStateCache.instance().persist();
				SyncStateCache.instance().disposeLastPersistedSyncState();
			}
			log.debug("Took {}ms to process batch synchronization. {}", 
					TimeUnit.NANOSECONDS.toMillis(end-start),
					success ? "No errors were encountered" : "Errors were encountered");
			return success;
		} catch (Exception e) {
			log.error("Error executing batch synchronization.", e);
		}
		return false;
	}
}
