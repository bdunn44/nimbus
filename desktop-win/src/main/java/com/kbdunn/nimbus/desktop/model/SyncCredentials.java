package com.kbdunn.nimbus.desktop.model;

public class SyncCredentials {
	
	private final String username, password, token;
	
	public SyncCredentials(String username, String password, String token) {
		this.username = username;
		this.password = password;
		this.token = token;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getCompositeString() {
		return getUsername() + ":" + getPassword() + ":" + getToken();
	}
	
	public static SyncCredentials fromCompositeString(String composite) throws IllegalArgumentException {
		String[] split = composite.split(":");
		if (split.length != 3) throw new IllegalArgumentException("Inavlid composite format");
		
		return new SyncCredentials(split[0], split[1], split[2]);
	}
}