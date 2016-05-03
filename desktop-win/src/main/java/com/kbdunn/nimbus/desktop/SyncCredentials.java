package com.kbdunn.nimbus.desktop;

import org.hive2hive.core.security.UserCredentials;

public class SyncCredentials {
	
	private UserCredentials credentials;
	
	public SyncCredentials(String username, String password, String pin) {
		credentials = new UserCredentials(username, password, pin);
	}
	
	public UserCredentials getUserCredentials() {
		return credentials;
	}
	
	public String getUsername() {
		return credentials.getUserId();
	}
	
	public String getPassword() {
		return credentials.getPassword();
	}
	
	public String getPin() {
		return credentials.getPin();
	}
	
	public String getCompositeString() {
		return getUsername() + ":" + getPassword() + ":" + getPin();
	}
	
	public static SyncCredentials fromCompositeString(String composite) throws IllegalArgumentException {
		String[] split = composite.split(":");
		if (split.length != 3) throw new IllegalArgumentException("Inavlid composite format");
		
		return new SyncCredentials(split[0], split[1], split[2]);
	}
}