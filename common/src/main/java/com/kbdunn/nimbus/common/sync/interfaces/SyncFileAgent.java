package com.kbdunn.nimbus.common.sync.interfaces;

import java.io.File;
import java.io.IOException;

public interface SyncFileAgent {
	File getSyncRoot();
	void writeCache(String key, byte[] data) throws IOException;
	byte[] readCache(String key) throws IOException;
}
