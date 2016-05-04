package com.kbdunn.nimbus.desktop.sync.listener;

import com.kbdunn.nimbus.common.sync.interfaces.FileAddEvent;
import com.kbdunn.nimbus.common.sync.interfaces.FileDeleteEvent;
import com.kbdunn.nimbus.common.sync.interfaces.FileEventListener;
import com.kbdunn.nimbus.common.sync.interfaces.FileMoveEvent;
import com.kbdunn.nimbus.common.sync.interfaces.FileUpdateEvent;
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
}
