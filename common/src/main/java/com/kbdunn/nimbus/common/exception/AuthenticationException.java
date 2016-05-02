package com.kbdunn.nimbus.common.exception;

public class AuthenticationException extends NimbusException {

	private static final long serialVersionUID = 9102088860250914130L;

	public AuthenticationException() {
		this("Unauthorized attempt to access resources!");
	}
	
	public AuthenticationException(String message) {
		super(message);
	}
}
