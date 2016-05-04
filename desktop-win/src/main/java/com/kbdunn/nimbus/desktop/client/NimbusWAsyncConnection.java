package com.kbdunn.nimbus.desktop.client;

import com.kbdunn.nimbus.common.sync.interfaces.FileManager;
import com.kbdunn.nimbus.desktop.model.SyncCredentials;

public class NimbusWAsyncConnection {

	// TODO: This entire class
	private FileManager fileManager;
	
	public NimbusWAsyncConnection() {
		
	}
	
	public boolean authenticate(SyncCredentials creds) {
		return true;
	}
	
	public boolean isConnected() {
		return true;
	}
	
	public boolean disconnect() {
		return true;
	}
	
	public FileManager getFileManager() {
		return fileManager;
	}
}
