package com.kbdunn.nimbus.desktop.client.sync.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.model.FileNode;

public class GetFileListprocess {

	private static final Logger log = LoggerFactory.getLogger(GetFileListprocess.class);
	
	private final IFileManager fileManager;
	
	public GetFileListprocess(IFileManager fileManager) {
		this.fileManager = fileManager;
	}

	public FileNode start() {
		IProcessComponent<FileNode> listFileProcess = null;
		try {
			listFileProcess = fileManager.createFileListProcess();
		} catch (NoPeerConnectionException | NoSessionException e) {
			log.error("Could not get the file list.", e);
			return null;
		}
		
		// Exceute synchronously
		try {
			return listFileProcess.execute();
		} catch (InvalidProcessStateException ex) {
			log.error("Could not launch the process to get the file list.", ex);
		} catch (ProcessExecutionException ex) {
			log.error("Process execution to get the file list failed.", ex);
		}
		return null;
	}
}
