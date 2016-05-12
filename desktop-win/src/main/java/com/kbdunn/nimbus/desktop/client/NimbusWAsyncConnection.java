package com.kbdunn.nimbus.desktop.client;

import com.kbdunn.nimbus.desktop.model.SyncCredentials;

public class NimbusWAsyncConnection {

	// TODO: This entire class
	private FileManager fileManager;
	
	public NimbusWAsyncConnection() {
		fileManager = new FileManager();
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
