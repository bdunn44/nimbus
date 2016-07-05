package com.kbdunn.nimbus.api.client.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=As.PROPERTY, property="@type")
public abstract class SyncEvent {

	private String originationId;
	
	public String getOriginationId() {
		return originationId;
	}
	
	public void setOriginationId(String originationId) {
		this.originationId = originationId;
	}
}
