package com.kbdunn.nimbus.desktop.sync;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.thirdparty.guava.common.util.concurrent.ThreadFactoryBuilder;
import com.kbdunn.nimbus.api.client.NimbusClient;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;
import com.kbdunn.nimbus.desktop.sync.listener.LocalFileEventListener;
import com.kbdunn.nimbus.desktop.sync.listener.RemoteFileEventListener;
import com.kbdunn.nimbus.desktop.sync.process.SynchronizeProcess;

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
	private RemoteFileEventListener remoteEventListener;
	private ScheduledExecutorService executor;
	
	private final AtomicBoolean batchSyncRunning;
	
	public DesktopSyncManager() {
		status = Status.DISCONNECTED;
		batchSyncRunning = new AtomicBoolean(false);
	}
	
	public Status getSyncStatus() {
		if (status.isConnected && !client.isConnectedToPushService()) {
			status = Status.CONNECTION_ERROR;
		}
		return status;
	}
	
	public ScheduledExecutorService getExecutor() {
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
				client.enablePushEventOriginationFilter();
				// Check REST API authentication
				if (!client.authenticate(TimeUnit.SECONDS.toMillis(5))) {
					throw new TransportException("Error authenticating " + creds.getUsername());
				}
				// Connect to PUSH. Don't listen to events until resume()
				if (!client.connectToPushService(TimeUnit.SECONDS.toMillis(5))) {
					throw new TransportException("Error connecting to push service");
				}
				status = Status.CONNECTED;
				log.info("Connected");
				
				// Subscribe to network change events
				fileManager = new RemoteFileManager(client);
				syncEventHandler = new SyncEventHandler(fileManager, this);
				
				// Listen to local and remote change events (not started until resume())
				fileObserver = new NimbusFileObserver(SyncPreferences.getSyncDirectory());
				fileObserver.addFileObserverListener(new LocalFileEventListener(syncEventHandler));
				remoteEventListener = new RemoteFileEventListener(syncEventHandler);
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
			if (executor != null) {
				executor.shutdown();
				executor.awaitTermination(2, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			log.warn("Sync tasks interrupted");
		} finally {
			if (executor != null) {
				if (!executor.isTerminated()) {
					log.warn("Sync tasks cancelled");
				}
				executor.shutdownNow();
			}
		}
		// Stop observing file events
		try {
			fileObserver.stop();
		} catch (Exception e) {
			log.error("Unable to stop file observer", e);
		}
		// Save current file state
		try {
			SyncStateCache.instance().persistCurrentState();
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
			executor = Executors.newScheduledThreadPool(WORKER_THREAD_COUNT, 
					new ThreadFactoryBuilder().setNameFormat("Sync Worker Thread #%d").build());
		}
		
		// Run in the application's background thread
		Application.asyncExec(() -> {
			// Run sychronization in batch, synchronously
			try {
				SyncStateCache.instance().rebuildCurrentState();
				batchSync();
			} catch (Exception e) {
				log.error("Uncaught exception encountered while running batch synchronization", e);
			}
			
			try {
				if (!client.isConnectedToPushService()) {
					log.warn("Client is no longer connected to the push service. Reconnecting...");
					if (!client.connectToPushService()) {
						throw new TransportException("Error connecting to push service");
					}
				}
				// Subscribe to pushed events
				fileManager.subscribeFileEvents(remoteEventListener);
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
			}
		});
	}
	
	private void batchSync() {
		if (batchSyncRunning.compareAndSet(false, true)) {
			status = Status.SYNCING;
			Application.updateSyncStatus();
			if (new SynchronizeProcess(syncEventHandler, fileManager).start()) {
				status = Status.SYNCED;
			} else {
				status = Status.SYNC_ERROR;
			}
			Application.updateSyncStatus();
			batchSyncRunning.set(false);
		}
	}
}
