package com.kbdunn.nimbus.common.exception;

public class EmailConflictException extends NimbusException {

	private static final long serialVersionUID = -3666935477154714977L;
	
	public EmailConflictException(String userEmail) {
		super("A user with the email address '" + userEmail + "' already exists.");
	}
}
