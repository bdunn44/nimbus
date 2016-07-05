package com.kbdunn.nimbus.desktop.sync.workers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.sync.RemoteFileManager;
import com.kbdunn.nimbus.desktop.sync.data.SyncStateCache;

public class BatchSyncRunner {
	
	private static final Logger log = LoggerFactory.getLogger(BatchSyncRunner.class);
	
	private final RemoteFileManager remoteFileManager;
	private final SyncEventHandler handler;
	
	public BatchSyncRunner(SyncEventHandler handler, RemoteFileManager remoteFileManager) {
		this.handler = handler;
		this.remoteFileManager = remoteFileManager;
	}
	
	public boolean start() {
		long start, end;
		start = System.nanoTime();
		log.info("Starting batch synchronization");
		
		// Wait for sync cache to be ready
		SyncStateCache.instance().awaitCacheReady();
		end = System.nanoTime();
		log.info("Waited {}ms for the sync state cache to be ready", TimeUnit.NANOSECONDS.toMillis(end-start));
		
		// Get the current list of network files
		List<SyncFile> networkFiles = null;
		try {
			networkFiles = remoteFileManager.createFileListProcess().call();
		} catch (Exception e) {
			log.error("Error encountered while retrieving network file list", e);
			return false;
		}
		
		// Process sync actions
		final BatchSyncArbiter arbiter = new BatchSyncArbiter(
				SyncStateCache.instance().getCurrentSyncState(), 
				SyncStateCache.instance().getLastPersistedSyncState(), 
				networkFiles,
				SyncStateCache.instance().getCurrentSyncErrors());
		arbiter.arbitrate();
		log.info("Took {}ms to make synchronization decisions", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-end));
		end = System.nanoTime();
		int errors = 0;
		
		// Add remote files
		for (SyncFile file : arbiter.getFilesToAddRemotely()) {
			try {
				handler.processLocalFileAddOrUpdate(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing remote file add {}", file, e);
			}
		}
		// Update remote files 
		for (SyncFile file : arbiter.getFilesToUpdateRemotely()) {
			try {
				handler.processLocalFileAddOrUpdate(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing remote file update {}", file, e);
			}
		}
		// Add local files
		for (SyncFile file : arbiter.getFilesToAddLocally()) {
			try {
				handler.processRemoteFileAdd(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing local file add {}", file, e);
			}
		}
		// Update local files
		for (SyncFile file : arbiter.getFilesToUpdateLocally()) {
			try {
				handler.processRemoteFileUpdate(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing local file update {}", file, e);
			}
		}
		// Copy Remote Files
		for (FileCopyEvent event : arbiter.getFilesToCopyRemotely()) {
			try {
				handler.processLocalFileCopy(event);
			} catch (Exception e) {
				errors++;
				log.error("Error processing remote file copy {}", event, e);
			}
		}
		// Copy Local Files
		for (FileCopyEvent event : arbiter.getFilesToCopyLocally()) {
			try {
				handler.processRemoteFileCopy(event);
			} catch (Exception e) {
				errors++;
				log.error("Error processing local file copy {}", event, e);
			}
		}
		// Delete local files
		for (SyncFile file : arbiter.getFilesToDeleteLocally()) {
			try {
				handler.processRemoteFileDelete(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing local file delete {}", file, e);
			}
		}
		// Delete remote files
		for (SyncFile file : arbiter.getFilesToDeleteRemotely()) {
			try {
				handler.processLocalFileDelete(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing remote file delete {}", file, e);
			}
		}
		// Handle sync conflicts
		for (SyncFile file : arbiter.getSyncConflicts()) {
			try {
				handler.processRemoteVersionConfict(file);
			} catch (Exception e) {
				errors++;
				log.error("Error processing sync conflict {}", file, e);
			}
		}
		
		if (errors > 0) {
			log.warn("{} batch sync error(s) were encountered", errors);
		}
		log.info("Finished batch synchronization in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start));
		return errors == 0;
	}
}