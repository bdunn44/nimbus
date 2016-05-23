package com.kbdunn.nimbus.api.client.model;

public class FileDeleteEvent extends FileEvent {

	public FileDeleteEvent() {  }

	public FileDeleteEvent(SyncFile file) {
		super(file);
	}

	@Override
	public String toString() {
		return "FileDeleteEvent [" + getFile() + "]";
	}
}