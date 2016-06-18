package com.kbdunn.nimbus.api.network;

import java.net.URI;

import com.kbdunn.nimbus.api.client.model.NimbusError;

public class NimbusResponse<T> {
	
	private final int status;
	private final T entity;
	private final NimbusError error;
	private final URI redirectURI;
	
	public NimbusResponse(int status, T entity) { 
		this.status = status; 
		this.entity = entity;
		this.error = null;
		this.redirectURI = null;
	}
	
	public NimbusResponse(int status, NimbusError error) {
		this.status = status;
		this.entity = null;
		this.error = error;
		this.redirectURI = null;
	}
	
	public NimbusResponse(int status, URI redirectURI) {
		this.status = status;
		this.entity = null;
		this.error = null;
		this.redirectURI = redirectURI;
		if (!this.redirected()) 
			throw new IllegalArgumentException("Only HTTP 3xx status codes are allow for redirections");
	}

	public int getStatus() {
		return status;
	}

	public boolean succeeded() {
		return (int) Math.floor(status/100) == 2;
	}
	
	public boolean redirected() {
		return (int) Math.floor(status/100) == 3;
	}
	
	public URI getRedirectURI() {
		return redirectURI;
	}
	
	public String getMessage() {
		if (!succeeded()) {
			return "Error processing request!";
		}
		return "Request was successfully processed.";
	}

	public T getEntity() {
		return entity;
	}
	
	public NimbusError getError() {
		return error;
	}

	@Override
	public String toString() {
		return "NimbusResponse [status=" + status + ", succeeded()=" + succeeded() + ", entity=" + entity + ", error=" + error + "]";
	}
}
