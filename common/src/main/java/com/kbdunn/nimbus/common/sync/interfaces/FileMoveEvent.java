package com.kbdunn.nimbus.common.sync.interfaces;

import com.kbdunn.nimbus.common.sync.model.SyncFile;

public interface FileMoveEvent extends FileEvent {
	SyncFile getSrcFile();
	SyncFile getDstFile();
}