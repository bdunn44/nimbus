package com.kbdunn.nimbus.api.client.model;

public class AuthenticateResponse {

	private String message;
	
	public AuthenticateResponse() {  }
	
	public AuthenticateResponse(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
