package com.kbdunn.nimbus.api.client.model;

public class FileUpdateEvent extends FileEvent {

	public FileUpdateEvent() {  }

	public FileUpdateEvent(SyncFile file) {
		super(file);
	}

	@Override
	public String toString() {
		return "FileUpdateEvent [file=" + getFile() + "]";
	}
}