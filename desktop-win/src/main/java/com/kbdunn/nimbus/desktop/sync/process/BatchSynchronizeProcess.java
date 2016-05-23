package com.kbdunn.nimbus.desktop.sync.process;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.sync.BatchFileSyncArbiter;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;
import com.kbdunn.nimbus.desktop.sync.SyncStateCache;

public class BatchSynchronizeProcess {
	
	private static final Logger log = LoggerFactory.getLogger(BatchSynchronizeProcess.class);
	
	private final SyncEventHandler syncHandler;
	//private final BatchFileSyncArbiter syncArbiter;
	
	private final List<SyncFile> toDeleteLocal;
	private final List<SyncFile> toDeleteRemote;
	private final List<SyncFile> toAddLocal;
	private final List<SyncFile> toAddRemote;
	private final List<SyncFile> toUpdateLocal;
	private final List<SyncFile> toUpdateRemote;
	private final List<SyncFile> versionConflicts;
	
	public BatchSynchronizeProcess(BatchFileSyncArbiter syncArbiter, SyncEventHandler syncHandler) {
		this.syncHandler = syncHandler;
		//this.syncArbiter = syncArbiter;
		
		this.toDeleteLocal = syncArbiter.getFilesToDeleteLocally();
		this.toDeleteRemote = syncArbiter.getFilesToDeleteRemotely();
		this.toAddLocal = syncArbiter.getFilesToAddLocally();
		this.toAddRemote = syncArbiter.getFilesToAddRemotely();
		this.toUpdateLocal = syncArbiter.getFilesToUpdateLocally();
		this.toUpdateRemote = syncArbiter.getFilesToUpdateRemotely();
		this.versionConflicts = syncArbiter.getVersionConflicts();
	}
	
	public boolean start() {
		try {
			// Wait for sync cache to be ready
			SyncStateCache.instance().awaitCacheReady();
			
			// Delete local files
			for (SyncFile file : toDeleteLocal) {
				syncHandler.handleRemoteFileDelete(file);
			}
			// Delete remote files
			for (SyncFile file : toDeleteRemote) {
				syncHandler.handleLocalFileDelete(file);
			}
			// Add remote files
			for (SyncFile file : toAddRemote) {
				log.debug("DEBUG! " + file);
				syncHandler.handleLocalFileAdd(file);
			}
			// Update remote files 
			for (SyncFile file : toUpdateRemote) {
				syncHandler.handleLocalFileUpdate(file);
			}
			// Add local files
			for (SyncFile file : toAddLocal) {
				syncHandler.handleRemoteFileAdd(file);
			}
			// Update local files
			for (SyncFile file : toUpdateLocal) {
				syncHandler.handleRemoteFileUpdate(file);
			}
			// Handle sync conflicts
			for (SyncFile file : versionConflicts) {
				syncHandler.handleRemoteVersionConfict(file);
			}
			return true;
		} catch (Exception e) {
			log.error("Batch synchronization error.", e);
			return false;
		}
	}
}