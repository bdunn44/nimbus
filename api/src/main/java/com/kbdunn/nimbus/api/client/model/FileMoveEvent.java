package com.kbdunn.nimbus.api.client.model;

public class FileMoveEvent extends FileEvent {
	
	private SyncFile dstFile;

	public FileMoveEvent() {  }
	
	public FileMoveEvent(SyncFile srcFile, SyncFile dstFile) {
		super(srcFile);
		this.dstFile = dstFile;
	}
	
	public SyncFile getSrcFile() {
		return super.getFile();
	}
	
	public SyncFile getDstFile() {
		return dstFile;
	}

	@Override
	public String toString() {
		return "FileMoveEvent [srcFile=" + getFile() + ", dstFile=" + dstFile + "]";
	}
}