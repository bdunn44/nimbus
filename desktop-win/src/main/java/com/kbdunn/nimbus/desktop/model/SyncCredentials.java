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
	
	public static SyncCredentials empty() {
		return new SyncCredentials("", "", "");
	}
	
	public static SyncCredentials fromCompositeString(String composite) throws IllegalArgumentException {
		if (composite == null || composite.isEmpty()) return SyncCredentials.empty();
		
		String[] split = composite.split(":");
		if (split.length != 3) throw new IllegalArgumentException("Inavlid composite format");
		
		return new SyncCredentials(split[0], split[1], split[2]);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((password == null) ? 0 : password.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SyncCredentials))
			return false;
		SyncCredentials other = (SyncCredentials) obj;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}