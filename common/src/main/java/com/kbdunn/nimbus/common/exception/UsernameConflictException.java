package com.kbdunn.nimbus.common.exception;

public class UsernameConflictException extends NimbusException {

	private static final long serialVersionUID = -3666935477154714977L;
	
	public UsernameConflictException(String userName) {
		super("A user with the name '" + userName + "' already exists.");
	}

}
