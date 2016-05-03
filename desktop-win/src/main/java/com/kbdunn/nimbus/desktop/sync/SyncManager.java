package com.kbdunn.nimbus.desktop.sync;

import java.io.IOException;
import java.net.InetAddress;

import org.hive2hive.core.api.H2HNode;
import org.hive2hive.core.api.configs.NetworkConfiguration;
import org.hive2hive.core.api.interfaces.IH2HNode;
import org.hive2hive.core.api.interfaces.INetworkConfiguration;
import org.hive2hive.core.processes.files.list.FileNode;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.SyncCredentials;
import com.kbdunn.nimbus.desktop.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.listener.LocalFileEventListener;
import com.kbdunn.nimbus.desktop.sync.listener.RemoteFileEventListener;
import com.kbdunn.nimbus.desktop.sync.process.BatchSynchronizeProcess;
import com.kbdunn.nimbus.desktop.sync.process.GetFileListprocess;

public class SyncManager {

	public enum Status {
		DISCONNECTED("Disconnected", false),
		CONNECTION_ERROR("Connection error", false),
		CONNECTED("Connected", true),
		SYNCING("Syncing Files...", true),
		SYNCED("Files Synced", true),
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
	
	private Status status;
	private IH2HNode node;
	private SyncEventHandler syncEventHandler;
	private NimbusFileObserver fileObserver;
	
	public SyncManager() {
		status = Status.DISCONNECTED;
	}
	
	public Status getSyncStatus() {
		return status;
	}
	
	public boolean connect() {
		log.info("Connecting to DHT...");
		try {
			INetworkConfiguration netConfig = NetworkConfiguration.create(SyncPreferences.getNodeName(), InetAddress.getByName(SyncPreferences.getEndpoint()));
			node = H2HNode.createNode(new NimbusH2HFileConfiguration());
			if (!node.connect(netConfig)) {
				throw new Exception("Error establishing connection to host " + SyncPreferences.getEndpoint());
			}
			SyncCredentials creds = SyncPreferences.getCredentials();
			if (!node.getUserManager().isRegistered(creds.getUsername())) {
				throw new Exception("User '" + creds.getUsername() + "' is not registered in the peer network");
			}
			node.getUserManager().createLoginProcess(creds.getUserCredentials(), new WindowsFileAgent()).execute();
			if (node.getUserManager().isLoggedIn()) {
				status = Status.CONNECTED;
				log.info("Connected");
			} else {
				throw new Exception("User authentication failed");
			}
			// Listen to network change events
			syncEventHandler = new SyncEventHandler(node.getFileManager(), this);
			
			// Listen to local change events (not started until resume())
			fileObserver = new NimbusFileObserver(Application.getSyncRootDirectory());
			fileObserver.addFileObserverListener(new LocalFileEventListener(syncEventHandler));
		} catch (Exception e) {
			status = Status.CONNECTION_ERROR;
			log.error("Unable to connect to DHT", e);
		} 
		Application.updateSyncStatus();
		
		// TEST
		new NetworkTester(node).printNetworkState();
		
		return status.isConnected();
	}
	
	public void disconnect() {
		if (node != null && node.isConnected()) {
			log.info("Disconnecting from DHT...");
			if (node.disconnect()) {
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
		try {
			FileSyncArbiter.persistCurrentSyncState();
		} catch (IOException e) {
			log.error("Unable to persist current sync state!", e);
		}
		try {
			fileObserver.stop();
		} catch (Exception e) {
			log.error("Unable to stop file observer", e);
		}
		status = Status.PAUSED;
		Application.updateSyncStatus();
	}
	
	public void resume() {
		if (!status.isConnected() || status == Status.SYNCING) {
			return;
		}
		log.info("Resuming file synchronization");
		status = Status.SYNCING;
		Application.updateSyncStatus();
		new Thread(new Runnable() {
			@Override
			public void run() {
				batchSynchronize();
				// If status is still "Syncing", update
				if (status == Status.SYNCING) {
					status = Status.SYNCED;
					Application.updateSyncStatus();
				}
				node.getFileManager().subscribeFileEvents(new RemoteFileEventListener(syncEventHandler));
				try {
					fileObserver.start();
				} catch (Exception e) {
					log.error("Unable to start file observer", e);
				}
			}
		}).start();
	}
	
	private synchronized boolean batchSynchronize() {
		FileNode rootNode = null;
		try {
			long start = System.currentTimeMillis();
			rootNode = new GetFileListprocess(node.getFileManager()).execute();
			long end = System.currentTimeMillis();
			log.debug("Took {}ms to fetch network file list.", end-start);
		} catch (InvalidProcessStateException | ProcessExecutionException e) {
			log.error("Error fetching network file list.", e);
			return false;
		}
		if (rootNode == null) {
			log.error("Error fetching network file list.");
			return false;
		} 
		
		try {
			long start = System.currentTimeMillis();
			FileSyncArbiter arbiter = new FileSyncArbiter(rootNode);
			BatchSynchronizeProcess batchSync = new BatchSynchronizeProcess(arbiter, syncEventHandler);
			boolean success = batchSync.execute();
			long end = System.currentTimeMillis();
			log.debug("Took {}ms to process batch synchronization.", end-start);
			if (success) {
				// Batch sync succeeded
				FileSyncArbiter.persistSyncState(arbiter.getCurrentSyncState());
				return true;
			}
		} catch (Exception e) {
			log.error("Error executing batch synchronization.", e);
		}
		return false;
	}
}
