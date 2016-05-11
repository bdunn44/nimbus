package com.kbdunn.nimbus.common.exception;

public class NimbusException extends Exception {

	private static final long serialVersionUID = -90547014904198263L;

	public NimbusException(String message) {
		super(message);
	}
	
	public NimbusException(Throwable cause) {
		super(cause);
	}
	
	public NimbusException(String message, Throwable cause) {
		super(message, cause);
	}
}
