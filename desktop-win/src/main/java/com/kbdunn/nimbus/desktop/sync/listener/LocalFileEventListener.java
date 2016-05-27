package com.kbdunn.nimbus.desktop.sync.listener;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.util.SyncFileUtil;
import com.kbdunn.nimbus.desktop.Application;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;
import com.kbdunn.nimbus.desktop.sync.SyncStateCache;

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
			handler.handleLocalFileAdd(SyncStateCache.instance().update(directory, true));
		} catch (IOException e) {
			log.error("Error encountered while processing directory create event '{}'", directory, e);
		}
	}
	
	@Override
	public void onDirectoryChange(File directory) {
		// Do nothing
	}

	@Override
	public void onDirectoryDelete(File directory) {
		try {
			SyncFile syncFile = SyncFileUtil.toSyncFile(SyncPreferences.getSyncDirectory(), directory, true);
			SyncStateCache.instance().delete(syncFile);
			handler.handleLocalFileDelete(syncFile);
		} catch (IOException e) {
			log.error("Error encountered while processing directory delete event '{}'", directory, e);
		}
	}
	
	@Override
	public void onFileCreate(File file) {
		try {
			if (file.canRead()) {
				handler.handleLocalFileAdd(SyncStateCache.instance().update(file, false));
			} else {
				// Something is still writing to it. Try again in 1s
				log.info("Detected file create but the file is currently open by another program ({})", file);
				Application.getSyncManager().getExecutor().schedule(() -> {
					onFileCreate(file);
				}, 1, TimeUnit.SECONDS);
			}
		} catch (IOException e) {
			log.error("Error encountered while processing file create event '{}'", file, e);
		}
	}
	
	@Override
	public void onFileChange(File file) {
		try {
			if (file.canRead()) {
				handler.handleLocalFileUpdate(SyncStateCache.instance().update(file, false));
			} else {
				// Something is still writing to it. Try again in 1s
				log.info("Detected file change but the file is currently open by another program ({})", file);
				Application.getSyncManager().getExecutor().schedule(() -> {
					onFileChange(file);
				}, 1, TimeUnit.SECONDS);
			}
		} catch (IOException e) {
			log.error("Error encountered while processing file change event '{}'", file, e);
		}
	}
	
	@Override
	public void onFileDelete(File file) {
		try {
			SyncFile syncFile = SyncFileUtil.toSyncFile(SyncPreferences.getSyncDirectory(), file, false);
			SyncStateCache.instance().delete(syncFile);
			handler.handleLocalFileDelete(syncFile);
		} catch (IOException e) {
			log.error("Error encountered while processing file delete event '{}'", file, e);
		}
	}
	
	@Override
	public void onStop(FileAlterationObserver observer) {
		// Do nothing
	}
	
	
}
