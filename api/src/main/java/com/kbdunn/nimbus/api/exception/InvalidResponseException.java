package com.kbdunn.nimbus.api.exception;

public class InvalidResponseException extends TransportException {
	
	private static final long serialVersionUID = -5117154419471957090L;

	public InvalidResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}