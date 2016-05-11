package com.kbdunn.nimbus.api.exception;

import com.kbdunn.nimbus.api.network.NimbusRequest;

public class InvalidRequestException extends TransportException {

	private static final long serialVersionUID = -1993038853528870466L;
	
	private final NimbusRequest<?> request;

	public InvalidRequestException(String message, NimbusRequest<?> request) {
		super(message);
		this.request = request;
	}
	
	public NimbusRequest<?> getRequest() {
		return request;
	}
}
