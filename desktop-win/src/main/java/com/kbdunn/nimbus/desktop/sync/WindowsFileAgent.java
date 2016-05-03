package com.kbdunn.nimbus.desktop.sync;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.hive2hive.core.file.IFileAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.desktop.SyncPreferences;

public class WindowsFileAgent implements IFileAgent {

	private static final Logger log = LoggerFactory.getLogger(WindowsFileAgent.class);
	
	@Override
	public File getRoot() {
		String dirname = SyncPreferences.getSyncDirectory();
		File dir = new File(dirname);
		if (dir.isFile()) {
			log.warn("Sync directory is a regular file! Renaming file to " + dirname + "-OLD");
			dir.renameTo(new File(dirname + "-OLD"));
			dir = new File(dirname);
		} 
		if (!dir.exists()) {
			log.warn("Sync directory does not exist. Creating folder " + dirname);
			if (!dir.mkdir()) {
				log.error("Unable to create folder " + dirname, new IOException());
				return null;
			}
		}
		return dir;
	}
	
	private File getCache() {
		return new File(FileUtils.getTempDirectory(), "Nimbus-Sync-Cache");
	}
	
	@Override
	public void writeCache(String key, byte[] data) throws IOException {
		FileUtils.writeByteArrayToFile(new File(getCache(), key), data);
	}
	
	@Override
	public byte[] readCache(String key) throws IOException {
		return FileUtils.readFileToByteArray(new File(getCache(), key));
	}
}
