package com.kbdunn.nimbus.server.google;

public class GmailMessage {
	
	private String raw;

	public GmailMessage(String raw) {
		this.raw = raw;
	}
	
	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
	}
}
