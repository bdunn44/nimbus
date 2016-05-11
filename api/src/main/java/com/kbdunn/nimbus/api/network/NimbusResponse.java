package com.kbdunn.nimbus.api.network;

import com.kbdunn.nimbus.api.model.NimbusError;

public class NimbusResponse<T> {
	
	private final int status;
	private final T entity;
	private final NimbusError error;
	
	public NimbusResponse(int status, T entity) { 
		this.status = status; 
		this.entity = entity;
		this.error = null;
	}
	
	public NimbusResponse(int status, NimbusError error) {
		this.status = status;
		this.entity = null;
		this.error = error;
	}

	public int getStatus() {
		return status;
	}

	public boolean succeeded() {
		return (int) Math.floor(status/100) == 2;
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
