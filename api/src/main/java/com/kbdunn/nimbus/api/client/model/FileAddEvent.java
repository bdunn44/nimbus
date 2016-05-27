package com.kbdunn.nimbus.api.client.model;

public class FileAddEvent extends FileEvent {

	public FileAddEvent() {  }
	
	public FileAddEvent(SyncFile file) {
		super(file);
	}

	@Override
	public String toString() {
		return "FileAddEvent [" + getFile() + ", originationId=" + super.getOriginationId() + "]";
	}
}
