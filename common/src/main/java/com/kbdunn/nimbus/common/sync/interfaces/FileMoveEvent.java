package com.kbdunn.nimbus.common.sync.interfaces;

import java.io.File;

public interface FileMoveEvent extends FileEvent {
	File getSrcFile();
	File getDstFile();
}