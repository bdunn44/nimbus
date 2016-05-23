package com.kbdunn.nimbus.desktop.sync.listener;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.util.DesktopSyncFileUtil;

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
		handler.handleLocalFileAdd(SyncFileUtil.toSyncFile(SyncPreferences.getSyncDirectory(), directory, true));
	}
	
	@Override
	public void onDirectoryChange(File directory) {
		// Do nothing
	}

	@Override
	public void onDirectoryDelete(File directory) {
		handler.handleLocalFileDelete(SyncFileUtil.toSyncFile(SyncPreferences.getSyncDirectory(), directory, true));
	}
	
	@Override
	public void onFileCreate(File file) {
		try {
			String md5 = StringUtil.bytesToHex(HashUtil.hash(file));
			handler.handleLocalFileAdd(DesktopSyncFileUtil.toSyncFile(file, md5));
		} catch (IOException e) {
			log.error("Error hashing locally created file " + file, e);
		}
	}
	
	@Override
	public void onFileChange(File file) {
		try {
			String md5 = StringUtil.bytesToHex(HashUtil.hash(file));
			handler.handleLocalFileUpdate(DesktopSyncFileUtil.toSyncFile(file, md5));
		} catch (IOException e) {
			log.error("Error hashing locally changed file " + file, e);
		}
	}
	
	@Override
	public void onFileDelete(File file) {
		handler.handleLocalFileDelete(SyncFileUtil.toSyncFile(SyncPreferences.getSyncDirectory(), file, false));
	}
	
	@Override
	public void onStop(FileAlterationObserver observer) {
		// Do nothing
	}
}
