package com.kbdunn.nimbus.common.exception;

public class ShareBlockNameConflictException extends NimbusException {

	private static final long serialVersionUID = 4522292803176295043L;
	
	public ShareBlockNameConflictException(String shareBlockName) {
		super("A user with the name '" + shareBlockName + "' already exists.");
	}
}