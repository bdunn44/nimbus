package com.kbdunn.nimbus.desktop.sync.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.client.RemoteFileManager;
import com.kbdunn.nimbus.desktop.sync.FileSyncArbiter;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;
import com.kbdunn.nimbus.desktop.sync.SyncStateCache;

public class SynchronizeProcess {
	
	private static final Logger log = LoggerFactory.getLogger(SynchronizeProcess.class);
	
	private final RemoteFileManager remoteFileManager;
	private final SyncEventHandler handler;
	
	public SynchronizeProcess(SyncEventHandler handler, RemoteFileManager remoteFileManager) {
		this.handler = handler;
		this.remoteFileManager = remoteFileManager;
	}
	
	public boolean start() {
		long start, end;
		start = System.nanoTime();
		log.info("Starting synchronization");
		
		// Wait for sync cache to be ready
		SyncStateCache.instance().awaitCacheReady();
		end = System.nanoTime();
		log.info("Waited {}ms for the sync state cache to be ready", TimeUnit.NANOSECONDS.toMillis(end-start));
		
		// Get state snapshots from the cache
		List<SyncFile> previousFiles = new ArrayList<>(SyncStateCache.instance().getLastPersistedSyncState().values());
		Map<String, SyncFile> currentState = SyncStateCache.instance().getCurrentSyncState();
		List<SyncFile> currentFiles = new ArrayList<>(currentState.values());
		log.info("Took {}ms to access the sync state cache", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		end = System.nanoTime();
		
		// Get the current list of network files
		List<SyncFile> networkFiles = null;
		try {
			networkFiles = remoteFileManager.createFileListProcess().call();
		} catch (Exception e) {
			log.error("Error encountered while retrieving network file list", e);
			return false;
		}
		log.info("Took {}ms to retrieve the current network file list", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-end));
		end = System.nanoTime();
		
		// Process sync actions
		final FileSyncArbiter arbiter = new FileSyncArbiter(currentFiles, previousFiles, networkFiles);
		arbiter.arbitrate();
		log.info("Took {}ms to make batch sync decisions", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-end));
		end = System.nanoTime();
		try {
			// Add remote files
			for (SyncFile file : arbiter.getFilesToAddRemotely()) {
				handler.processLocalFileAddOrUpdate(file);
			}
			// Update remote files 
			for (SyncFile file : arbiter.getFilesToUpdateRemotely()) {
				handler.processLocalFileAddOrUpdate(file);
			}
			// Add local files
			for (SyncFile file : arbiter.getFilesToAddLocally()) {
				handler.processRemoteFileAdd(file);
			}
			// Update local files
			for (SyncFile file : arbiter.getFilesToUpdateLocally()) {
				handler.processRemoteFileUpdate(file);
			}
			// Copy Remote Files
			for (FileCopyEvent event : arbiter.getFilesToCopyRemotely()) {
				handler.processLocalFileCopy(event);
			}
			// Copy Local Files
			for (FileCopyEvent event : arbiter.getFilesToCopyLocally()) {
				handler.processRemoteFileCopy(event);
			}
			// Delete local files
			for (SyncFile file : arbiter.getFilesToDeleteLocally()) {
				handler.processRemoteFileDelete(file);
			}
			// Delete remote files
			for (SyncFile file : arbiter.getFilesToDeleteRemotely()) {
				handler.processLocalFileDelete(file);
			}
			// Handle sync conflicts
			for (SyncFile file : arbiter.getSyncConflicts()) {
				handler.processRemoteVersionConfict(file);
			}
		} catch (Exception e) {
			log.error("Encountered a synchronization error.", e);
			return false;
		}
		
		// Persist the processed sync state
		try {
			SyncStateCache.instance().persist(currentState);
		} catch (IOException e) {
			log.error("Error encountered while persisting the sync state", e);
			return false;
		}
		
		log.info("Finished synchronization in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		return true;
	}
}