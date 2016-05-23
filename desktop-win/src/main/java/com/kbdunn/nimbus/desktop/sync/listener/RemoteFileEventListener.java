package com.kbdunn.nimbus.desktop.sync.listener;

import com.kbdunn.nimbus.api.client.listeners.FileEventListener;
import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileCopyEvent;
import com.kbdunn.nimbus.api.client.model.FileDeleteEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.FileUpdateEvent;
import com.kbdunn.nimbus.desktop.sync.SyncEventHandler;

public class RemoteFileEventListener implements FileEventListener {
	
	private final SyncEventHandler handler;
	
	public RemoteFileEventListener(SyncEventHandler handler) {
		this.handler = handler;
	}
	
	@Override
	public void onFileAdd(FileAddEvent fileEvent) {
		handler.handleRemoteFileAdd(fileEvent.getFile());
	}
	
	@Override
	public void onFileUpdate(FileUpdateEvent fileEvent) {
		handler.handleRemoteFileUpdate(fileEvent.getFile());
	}
	
	@Override
	public void onFileDelete(FileDeleteEvent fileEvent) {
		handler.handleRemoteFileDelete(fileEvent.getFile());
	}
	
	@Override
	public void onFileMove(FileMoveEvent fileEvent) {
		handler.handleRemoteFileMove(fileEvent.getSrcFile(), fileEvent.getDstFile());
	}

	@Override
	public void onFileCopy(FileCopyEvent fileEvent) {
		handler.hadleRemoteFileCopy(fileEvent.getSrcFile(), fileEvent.getDstFile());
	}
}
