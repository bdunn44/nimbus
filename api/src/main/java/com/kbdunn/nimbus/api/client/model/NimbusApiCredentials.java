package com.kbdunn.nimbus.api.client.model;

public class NimbusApiCredentials {

	private final String apiToken;
	private final String hmacKey;
	
	public NimbusApiCredentials(String apiToken, String hmacKey) {
		this.apiToken = apiToken;
		this.hmacKey = hmacKey;
	}

	public String getApiToken() {
		return apiToken;
	}

	public String getHmacKey() {
		return hmacKey;
	}
}
