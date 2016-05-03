package com.kbdunn.nimbus.common.sync.interfaces;

import java.io.File;

public interface FileEvent {
	File getFile();
	boolean isFile();
	boolean isFolder();
}