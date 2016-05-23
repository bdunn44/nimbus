package com.kbdunn.nimbus.desktop.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.NimbusApiCredentials;

public class SyncCredentials {
	
	private static final Logger log = LoggerFactory.getLogger(SyncCredentials.class);
	
	private final String username, apiToken, hmacKey;
	
	public SyncCredentials(String username, String apiToken, String hmacKey) {
		this.username = username;
		this.apiToken = apiToken;
		this.hmacKey = hmacKey;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getApiToken() {
		return apiToken;
	}
	
	public String getHmacKey() {
		return hmacKey;
	}
	
	public String getCompositeString() {
		return getUsername() + "::" + getApiToken() + "::" + getHmacKey();
	}
	
	public NimbusApiCredentials toNimbusApiCredentials() {
		return new NimbusApiCredentials(apiToken, hmacKey);
	}
	
	public static SyncCredentials empty() {
		return new SyncCredentials("", "", "");
	}
	
	public static SyncCredentials fromCompositeString(String composite) throws IllegalArgumentException {
		if (composite == null || composite.isEmpty()) return SyncCredentials.empty();
		
		String[] split = composite.split("::");
		if (split.length != 3) {
			log.error("Encountered invalid composite credential format. Clearing credentials");
			return SyncCredentials.empty();
		}
		
		return new SyncCredentials(split[0], split[1], split[2]);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hmacKey == null) ? 0 : hmacKey.hashCode());
		result = prime * result + ((apiToken == null) ? 0 : apiToken.hashCode());
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
		if (hmacKey == null) {
			if (other.hmacKey != null)
				return false;
		} else if (!hmacKey.equals(other.hmacKey))
			return false;
		if (apiToken == null) {
			if (other.apiToken != null)
				return false;
		} else if (!apiToken.equals(other.apiToken))
			return false;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}
}