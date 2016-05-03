package com.kbdunn.nimbus.desktop.sync.process;

import org.hive2hive.core.api.interfaces.IFileManager;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.processes.files.list.FileNode;
import org.hive2hive.processframework.ProcessStep;
import org.hive2hive.processframework.exceptions.InvalidProcessStateException;
import org.hive2hive.processframework.exceptions.ProcessExecutionException;
import org.hive2hive.processframework.interfaces.IProcessComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GetFileListprocess extends ProcessStep<FileNode> {

	private static final Logger log = LoggerFactory.getLogger(GetFileListprocess.class);
	
	private final IFileManager fileManager;
	
	public GetFileListprocess(IFileManager fileManager) {
		this.fileManager = fileManager;
	}

	@Override
	protected FileNode doExecute() throws InvalidProcessStateException, ProcessExecutionException {
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
