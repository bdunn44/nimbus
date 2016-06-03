package com.kbdunn.nimbus.api.client.model;

public class NimbusApiCredentials {

	private final String username;
	private final String apiToken;
	
	public NimbusApiCredentials(String username, String apiToken) {
		this.username = username;
		this.apiToken = apiToken;
	}
	
	public String getUsername() {
		return username;
	}

	public String getApiToken() {
		return apiToken;
	}
}
