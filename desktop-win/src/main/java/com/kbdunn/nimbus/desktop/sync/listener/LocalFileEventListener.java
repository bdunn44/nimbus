package com.kbdunn.nimbus.desktop.sync.listener;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;

public class LocalFileEventListener implements FileAlterationListener {
	
	private final SyncEventHandler handler;
	
	public LocalFileEventListener(SyncEventHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void onStart(FileAlterationObserver observer) {
		// Do nothing
	}

	@Override
	public void onDirectoryCreate(File directory) {
		handler.handleLocalFileAdd(directory);
	}

	@Override
	public void onDirectoryChange(File directory) {
		// Do nothing
	}

	@Override
	public void onDirectoryDelete(File directory) {
		handler.handleLocalFileDelete(directory);
	}
	
	@Override
	public void onFileCreate(File file) {
		handler.handleLocalFileAdd(file);
	}
	
	@Override
	public void onFileChange(File file) {
		handler.handleLocalFileUpdate(file);
	}
	
	@Override
	public void onFileDelete(File file) {
		handler.handleLocalFileDelete(file);
	}
	
	@Override
	public void onStop(FileAlterationObserver observer) {
		// Do nothing
	}
}
