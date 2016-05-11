package com.kbdunn.nimbus.api.network;

import java.net.MalformedURLException;
import java.net.URL;

public class NimbusConnection {

	public static final String API_PATH = "/request";
	
	private URL url;
	private String apiToken;
	private String hmacKey;
	
	public NimbusConnection(String url, String apiToken, String hmacKey) throws MalformedURLException {
		this.url = new URL(url);
		this.apiToken = apiToken;
		this.hmacKey = hmacKey;
	}
	
	public URL getUrl() {
		return url;
	}
	
	public void setUrl(URL url) {
		this.url = url;
	}
	
	public String getApiEndpoint() {
		if (url == null) return null;
		return url.getProtocol() + "://" + url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort()) + API_PATH;
	}
	
	public String getApiToken() {
		return apiToken;
	}
	
	public void setApiToken(String apiKey) {
		this.apiToken = apiKey;
	}
	
	public String getHmacKey() {
		return hmacKey;
	}
	
	public void setHmacKey(String hmacKey) {
		this.hmacKey = hmacKey;
	}
}
