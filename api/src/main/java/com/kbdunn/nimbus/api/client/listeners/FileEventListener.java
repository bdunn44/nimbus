package com.kbdunn.nimbus.api.client.listeners;

import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileDeleteEvent;
import com.kbdunn.nimbus.api.client.model.FileMoveEvent;
import com.kbdunn.nimbus.api.client.model.FileUpdateEvent;

public interface FileEventListener {
	void onFileAdd(FileAddEvent fileEvent);
	void onFileUpdate(FileUpdateEvent fileEvent);
	void onFileDelete(FileDeleteEvent fileEvent);
	void onFileMove(FileMoveEvent fileEvent);
}