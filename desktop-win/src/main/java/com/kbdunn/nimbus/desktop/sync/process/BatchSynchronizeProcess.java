package com.kbdunn.nimbus.desktop.sync.process;

import java.io.File;
import java.util.List;

import org.hive2hive.processframework.ProcessStep;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.sync.FileSyncArbiter;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;

public class BatchSynchronizeProcess extends ProcessStep<Boolean> {
	
	private static final Logger log = LoggerFactory.getLogger(BatchSynchronizeProcess.class);
	
	private final SyncEventHandler syncHandler;
	
	private final List<File> toDeleteLocal;
	private final List<File> toDeleteRemote;
	private final List<File> toAddLocal;
	private final List<File> toAddRemote;
	private final List<File> toUpdateLocal;
	private final List<File> toUpdateRemote;
	private final List<File> remoteVersionConflicts;
	
	public BatchSynchronizeProcess(FileSyncArbiter syncArbiter, SyncEventHandler syncHandler) {
		this.syncHandler = syncHandler;
		
		this.toDeleteLocal = syncArbiter.getFilesToDeleteLocally();
		this.toDeleteRemote = syncArbiter.getFilesToDeleteRemotely();
		this.toAddLocal = syncArbiter.getFilesToAddLocally();
		this.toAddRemote = syncArbiter.getFilesToAddRemotely();
		this.toUpdateLocal = syncArbiter.getFilesToUpdateLocally();
		this.toUpdateRemote = syncArbiter.getFilesToUpdateRemotely();
		this.remoteVersionConflicts = syncArbiter.getRemoteVersionConflicts();
	}
	
	@Override
	protected Boolean doExecute() throws InvalidProcessStateException, ProcessExecutionException {
		try {
			// Delete local files
			for (File file : toDeleteLocal) {
				syncHandler.handleLocalFileDelete(file);
			}
			// Delete remote files
			for (File file : toDeleteRemote) {
				syncHandler.handleRemoteFileDelete(file);
			}
			// Add remote files
			for (File file : toAddRemote) {
				syncHandler.handleRemoteFileAdd(file);
			}
			// Update remote files 
			for (File file : toUpdateRemote) {
				syncHandler.handleRemoteFileUpdate(file);
			}
			// Add local files
			for (File file : toAddLocal) {
				syncHandler.handleLocalFileAdd(file);
			}
			// Update local files
			for (File file : toUpdateLocal) {
				syncHandler.handleLocalFileUpdate(file);
			}
			// Handle sync conflicts
			for (File file : remoteVersionConflicts) {
				syncHandler.handleRemoteVersionConfict(file);
			}
			return true;
		} catch (Exception e) {
			log.error("Batch synchronization error.", e);
			return false;
		}
	}
}