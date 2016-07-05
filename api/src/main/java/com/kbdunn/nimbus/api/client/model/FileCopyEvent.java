package com.kbdunn.nimbus.api.client.model;

public class FileCopyEvent extends FileMoveEvent {
	
	public FileCopyEvent() {  }
	
	public FileCopyEvent(SyncFile srcFile, SyncFile dstFile) {
		super(srcFile, dstFile);
	}
	
	public FileCopyEvent(SyncFile srcFile, SyncFile dstFile, boolean replaceExistingFile) {
		super(srcFile, dstFile, replaceExistingFile);
	}
	
	@Override
	public String toString() {
		return "FileCopyEvent [source=" + getFile() + ", target=" + getDstFile() 
		+ ", replace=" + isReplaceExistingFile() + ", originationId=" + super.getOriginationId() + "]";
	}
}