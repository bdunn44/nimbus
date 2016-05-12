package com.kbdunn.nimbus.desktop.sync.listener;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;

public class LocalFileEventListener implements FileAlterationListener {
	
	private static final Logger log = LoggerFactory.getLogger(LocalFileEventListener.class);
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
		try {
			handler.handleLocalFileAdd(SyncFileUtil.toSyncFile(Application.getSyncRootDirectory(), directory, true));
		} catch (IOException e) {
			log.error("Error handling directory create event!", e);
		}
	}

	@Override
	public void onDirectoryChange(File directory) {
		// Do nothing
	}

	@Override
	public void onDirectoryDelete(File directory) {
		try {
			handler.handleLocalFileDelete(SyncFileUtil.toSyncFile(Application.getSyncRootDirectory(), directory, true));
		} catch (IOException e) {
			log.error("Error handling directory delete event!", e);
		}
	}
	
	@Override
	public void onFileCreate(File file) {
		try {
			handler.handleLocalFileAdd(SyncFileUtil.toSyncFile(Application.getSyncRootDirectory(), file, false));
		} catch (IOException e) {
			log.error("Error handling file create event!", e);
		}
	}
	
	@Override
	public void onFileChange(File file) {
		try {
			handler.handleLocalFileUpdate(SyncFileUtil.toSyncFile(Application.getSyncRootDirectory(), file, false));
		} catch (IOException e) {
			log.error("Error handling file change event!", e);
		}
	}
	
	@Override
	public void onFileDelete(File file) {
		try {
			handler.handleLocalFileDelete(SyncFileUtil.toSyncFile(Application.getSyncRootDirectory(), file, false));
		} catch (IOException e) {
			log.error("Error handling file delete event!", e);
		}
	}
	
	@Override
	public void onStop(FileAlterationObserver observer) {
		// Do nothing
	}
}
