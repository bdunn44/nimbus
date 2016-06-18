package com.kbdunn.nimbus.api.client.model;

public class FileMoveEvent extends FileEvent {
	
	private SyncFile dstFile;
	private boolean replaceExistingFile;

	public FileMoveEvent() {  }
	
	public FileMoveEvent(SyncFile srcFile, SyncFile dstFile) {
		super(srcFile);
		this.dstFile = dstFile;
		this.replaceExistingFile = false;
	}
	
	public FileMoveEvent(SyncFile srcFile, SyncFile dstFile, boolean replaceExistingFile) {
		super(srcFile);
		this.dstFile = dstFile;
		this.replaceExistingFile = replaceExistingFile;
	}
	
	public SyncFile getSrcFile() {
		return super.getFile();
	}
	
	public SyncFile getDstFile() {
		return dstFile;
	}
	
	public boolean isReplaceExistingFile() {
		return replaceExistingFile;
	}

	@Override
	public String toString() {
		return "FileMoveEvent [source=" + getFile() + ", target=" + dstFile 
				+ ", replace=" + replaceExistingFile + ", originationId=" + super.getOriginationId() + "]";
	}
}