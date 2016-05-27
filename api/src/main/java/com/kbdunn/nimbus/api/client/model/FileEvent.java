package com.kbdunn.nimbus.api.client.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=As.PROPERTY, property="@type")
public abstract class FileEvent {
	
	private SyncFile file;
	private String originationId;
	
	public FileEvent() {  }
	
	public FileEvent(SyncFile file) {
		this.file = file;
	}
	
	public SyncFile getFile() {
		return file;
	}
	
	public String getOriginationId() {
		return originationId;
	}
	
	public void setOriginationId(String originationId) {
		this.originationId = originationId;
	}

	@Override
	public String toString() {
		return "FileEvent [" + file + ", originationId=" + originationId + "]";
	}
}