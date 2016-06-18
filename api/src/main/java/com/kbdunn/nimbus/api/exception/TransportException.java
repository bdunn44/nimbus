package com.kbdunn.nimbus.api.exception;

import com.kbdunn.nimbus.common.exception.NimbusException;

public class TransportException extends NimbusException {

	private static final long serialVersionUID = -2435763297391711327L;

	public TransportException(String message, Throwable cause) {
		super(message, cause);
	}

	public TransportException(String message) {
		super(message);
	}
}
