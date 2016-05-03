package com.kbdunn.nimbus.desktop.client.sync;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hive2hive.client.util.buffer.AddFileBuffer;
import org.hive2hive.client.util.buffer.DeleteFileBuffer;
import org.hive2hive.client.util.buffer.ModifyFileBuffer;
import org.hive2hive.core.api.interfaces.IFileManager;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.hive2hive.processframework.interfaces.IProcessComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.Application;

public class SyncEventHandler {

	private static final Logger log = LoggerFactory.getLogger(SyncEventHandler.class);
	
	private final IFileManager fileManager;
	private final SyncManager syncManager;
	
	// Buffers to capture local change events
	private final AddFileBuffer addFileBuffer;
	private final DeleteFileBuffer deleteFileBuffer;
	private final ModifyFileBuffer modifyFileBuffer;
	
	public SyncEventHandler(IFileManager fileManager, SyncManager syncManager) {
		this.fileManager = fileManager;
		this.syncManager = syncManager;
		this.addFileBuffer = new AddFileBuffer(fileManager);
		this.deleteFileBuffer = new DeleteFileBuffer(fileManager);
		this.modifyFileBuffer = new ModifyFileBuffer(fileManager);
	}
	
	public void handleLocalFileAdd(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created locally");
		addFileBuffer.addFileToBuffer(file);
	}
	
	public void handleRemoteFileAdd(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "created remotely");
		try {
			IProcessComponent<Void> downloadProcess = fileManager.createDownloadProcess(file);
			downloadProcess.execute();
		} catch (InvalidProcessStateException | ProcessExecutionException | NoSessionException | NoPeerConnectionException e) {
			log.error("Cannot download the new file {}", file.getAbsolutePath());
		}
	}
	
	public void handleLocalFileUpdate(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "modified locally");
		modifyFileBuffer.addFileToBuffer(file);
	}
	
	public void handleRemoteFileUpdate(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "modified remotely");
		try {
			IProcessComponent<Void> downloadProcess = fileManager.createDownloadProcess(file);
			downloadProcess.execute();
		} catch (InvalidProcessStateException | ProcessExecutionException | NoSessionException | NoPeerConnectionException e) {
			log.error("Cannot download the updated file {}", file.getAbsolutePath());
		}
	}
	
	public void handleLocalFileDelete(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted locally");
		deleteFileBuffer.addFileToBuffer(file);
	}
	
	public void handleRemoteFileDelete(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "deleted remotely");
		if (file.delete()) {
			log.error("Deleted file " + file);
		} else {
			log.error("Could not delete file {}", file.getAbsolutePath());
		}
	}
	
	public void handleRemoteFileMove(File src, File dest) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(src, "moved remotely");
		try {
			if (src.isFile()) {
				FileUtils.moveFile(src, dest);
			} else {
				FileUtils.moveDirectory(src, dest);
			}
		} catch (IOException e) {
			log.error("Error moving {} {} to {}", src.isDirectory() ? "folder" : "file", src.getAbsolutePath(), dest.getAbsolutePath());
		}
	}
	
	public void handleRemoteVersionConfict(File file) {
		if (!syncManager.isSyncActive()) return;
		logChangeEvent(file, "has a remote version conflict");
		try {
			// Rename the local file
			File local = new File(Application.getSyncRootDirectory(), file.getPath());
			String rename = local.getName() + "-" + SyncPreferences.getNodeName();
			log.info("Renaming local file {} to {}", local.getAbsolutePath(), rename);
			if (!local.renameTo(new File(Application.getSyncRootDirectory(), rename))) {
				log.warn("Unable to rename file {}", local.getAbsolutePath());
				// TODO sync error icon on file
				return;
			}
			
			// Download the network file
			IProcessComponent<Void> downloadProcess = fileManager.createDownloadProcess(file);
			downloadProcess.execute();
		} catch (InvalidProcessStateException | ProcessExecutionException | NoSessionException | NoPeerConnectionException e) {
			log.error("Cannot download the remote file {}", file.getAbsolutePath());
		}
	}
	
	private void logChangeEvent(File file, String event) {
		log.info("{} {}: {}", file.isDirectory() ? "Directory" : "File", event, file.getAbsolutePath());
	}
}
