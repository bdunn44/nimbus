package com.kbdunn.nimbus.desktop.sync;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kbdunn.nimbus.api.client.NimbusClient;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.common.util.TrackedScheduledThreadPoolExecutor;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.ApplicationProperties;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;
import com.kbdunn.nimbus.desktop.sync.data.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.data.SyncStateCache;
import com.kbdunn.nimbus.desktop.sync.listeners.LocalFileEventListener;
import com.kbdunn.nimbus.desktop.sync.listeners.RemoteFileEventListener;
import com.kbdunn.nimbus.desktop.sync.workers.BatchSyncRunner;
import com.kbdunn.nimbus.desktop.sync.workers.SyncEventHandler;

public class SyncManager {

	public enum Status {
		DISCONNECTED("Disconnected", false),
		CONNECTING("Connecting...", false),
		CONNECTED("Connected", true),
		CONNECTION_ERROR("Connection error", false),
		SYNCING("Processing {}...", true),
		SYNCED("Files synced", true),
		SYNC_ERROR("Failed to process {}", true),
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
	private static final int WORKER_THREAD_COUNT = 1;
	
	private Status status;
	private NimbusClient client;
	private RemoteFileManager fileManager;
	private SyncEventHandler syncEventHandler;
	private NimbusFileObserver fileObserver;
	private RemoteFileEventListener remoteEventListener;
	private LocalFileEventListener localEventListener;
	private TrackedScheduledThreadPoolExecutor executor;
	
	private final AtomicBoolean batchSyncRunning;
	
	public SyncManager() {
		status = Status.DISCONNECTED;
		batchSyncRunning = new AtomicBoolean(false);
	}
	
	public Status getSyncStatus() {
		if (status.isConnected && status != Status.SYNCING && !client.isConnectedToPushService()) {
			pause();
			status = Status.CONNECTION_ERROR;
		}
		if (isSyncActive()) {
			status = getSyncTaskCount() > 0 ? Status.SYNCING : Status.SYNCED;
			if (status == Status.SYNCED && SyncStateCache.instance().getCurrentSyncErrors().size() > 0) {
				status = Status.SYNC_ERROR;
			}
		}
		return status;
	}
	
	public TrackedScheduledThreadPoolExecutor getExecutor() {
		return executor;
	}
	
	public boolean isSyncActive() {
		return status.isConnected() 
				&& status != Status.PAUSED
				&& status != Status.CONNECTED // CONNECTED is not actively syncing
			;
	}
	
	public long getSyncTaskCount() {
		return executor == null ? 0l : executor.getQueueSize() + executor.getActiveCount();
	}
	
	public boolean connect() {
		final SyncCredentials creds = SyncPreferences.getCredentials();
		String url = SyncPreferences.getUrl();
		if (creds.equals(SyncCredentials.empty()) || url == null || url.isEmpty()) {
			// Don't even try
			Application.showNotification("Log in to your cloud to synchronize your files");
			Application.openSettingsWindow();
			status = Status.DISCONNECTED;
			Application.updateSyncStatus();
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
				// Set the URL to the one resolved by the client. It may have been redirected.
				url = client.getUrlString();
				SyncPreferences.setUrl(url);
				// Connect to PUSH. Don't listen to events until resume()
				if (!client.connectToPushService(TimeUnit.SECONDS.toMillis(5))) {
					throw new TransportException("Error connecting to push service");
				}
				status = Status.CONNECTED;
				log.info("Connected");
				
				// Listen to local and remote change events (not started until resume())
				fileManager = new RemoteFileManager(client);
				syncEventHandler = new SyncEventHandler(fileManager, this);
				remoteEventListener = new RemoteFileEventListener(syncEventHandler);
				if (fileObserver == null) {
					fileObserver = new NimbusFileObserver(ApplicationProperties.instance().getSyncDirectory());
				}
				if (localEventListener == null) {
					localEventListener = new LocalFileEventListener(syncEventHandler);
				} else {
					localEventListener.setSyncEventHandler(syncEventHandler);
				}
				fileObserver.addFileObserverListener(localEventListener);
			} catch (Exception e) {
				status = Status.CONNECTION_ERROR;
				log.error("Unable to connect to Nimbus", e);
			} finally {
				Application.updateSyncStatus();
			}
		}
		
		return status.isConnected();
	}
	
	public void resume() {
		if (!status.isConnected() || status == Status.SYNCING) {
			return;
		}
		log.info("Resuming file synchronization");
		// Run in the application's background thread
		Application.asyncExec(() -> {
			// Initialize executor
			if (executor == null || executor.isTerminated()) {
				executor = new TrackedScheduledThreadPoolExecutor(WORKER_THREAD_COUNT, 
								new ThreadFactoryBuilder().setNameFormat("Sync Worker Thread #%d").build());
			}
			// Run sychronization in batch, synchronously
			try {
				batchSync();
			} catch (Exception e) {
				log.error("Uncaught exception encountered while running batch synchronization", e);
			}
			Application.updateSyncStatus();
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
	
	public void pause() {
		if (!status.isConnected()) {
			return;
		}
		log.info("Pausing file synchronization");
		// Shutdown thread pool, etc.
		releaseResources();
		try {
			// Save current file state
			SyncStateCache.instance().persistCurrentState();
		} catch (IOException e) {
			log.error("Unable to persist current sync state!", e);
		}
		
		status = Status.PAUSED;
		Application.updateSyncStatus();
	}
	
	public void disconnect() {
		// Check that we're connected, if so, disconnect
		log.info("Disconnecting from Nimbus...");
		client.disconnect();
		log.info("Disconnected");
		status = Status.DISCONNECTED;
		Application.updateSyncStatus();
	}
	
	private void releaseResources() {
		long start = System.nanoTime();
		// Unsubscribe from push events
		log.debug("Unsubscribing from remote events");
		fileManager.unsubscribeAll();
		
		// Shutdown executor
		log.debug("Shutting down worker thread pool");
		try {
			if (executor != null) {
				executor.shutdown();
				executor.awaitTermination(1, TimeUnit.SECONDS);
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
		log.debug("Stopping the local file observer");
		try {
			fileObserver.stop();
		} catch (Exception e) {
			log.error("Unable to stop the file observer", e);
		}
		log.debug("Took {}ms to release resources", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
	}
	
	private void batchSync() throws InterruptedException, ExecutionException {
		if (batchSyncRunning.compareAndSet(false, true)) {
			// Submit in sync worker thread pool to show an active sync task
			executor.submit(() -> {
				try {
					status = Status.SYNCING;
					Application.updateSyncStatus();
					SyncStateCache.instance().rebuildCurrentState();
					if (new BatchSyncRunner(syncEventHandler, fileManager).start()) {
						status = Status.SYNCED;
					} else {
						status = Status.SYNC_ERROR;
					}
				} catch (IOException e) {
					log.error("Failed to rebuild sync state cache", e);
					status = Status.SYNC_ERROR;
					return;
				} finally {
					batchSyncRunning.set(false);
				}
			}).get();
		}
	}
}
