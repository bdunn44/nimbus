package com.kbdunn.nimbus.api.client.model;

public class FileCopyEvent extends FileMoveEvent {
	
	public FileCopyEvent() {  }
	
	public FileCopyEvent(SyncFile srcFile, SyncFile dstFile) {
		super(srcFile, dstFile);
	}

	@Override
	public String toString() {
		return "FileCopyEvent [source=" + getSrcFile() + ", target=" + getDstFile() + "]";
	}
}