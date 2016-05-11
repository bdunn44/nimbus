package com.kbdunn.nimbus.api.model;

public class NimbusError {
	
	private String message;
	
	public NimbusError() {  }
	
	public NimbusError(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return "NimbusError [message=" + message + "]";
	}
}
