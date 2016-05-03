package com.kbdunn.nimbus.common.sync.interfaces;

public interface FileEventListener {
	void onFileAdd(FileAddEvent fileEvent);
	void onFileUpdate(FileUpdateEvent fileEvent);
	void onFileDelete(FileDeleteEvent fileEvent);
	void onFileMove(FileMoveEvent fileEvent);
}