package com.kbdunn.nimbus.desktop.sync.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.ApplicationProperties;

public final class DesktopSyncFileUtil {
	
	private static final Logger log = LoggerFactory.getLogger(DesktopSyncFileUtil.class);
	
	private DesktopSyncFileUtil() {  }
	
	public static File toFile(SyncFile syncFile) {
		File syncDir = ApplicationProperties.instance().getSyncDirectory();
		if (!syncDir.isDirectory()) throw new IllegalStateException("Sync root directory does not exist: " + syncDir);
		return new File(syncDir, syncFile.getPath());
	}
	
	public static Map<String, SyncFile> buildMap(List<SyncFile> syncFiles) {
		HashMap<String, SyncFile> map = new HashMap<>();
		for (SyncFile syncFile : syncFiles) {
			map.put(syncFile.getPath(), syncFile);
		}
		return map;
	}
	
	public static String toCompositeString(SyncFile file) {
		return file.getPath() 
				+ "::" + file.getMd5() 
				+ "::" + file.isDirectory() 
				+ "::" + file.getSize() 
				+ "::" + file.getLastHashed() 
				+ "::" + file.getLastModified();
	}
	
	public static SyncFile fromCompositeString(String composite) {
		String[] split = composite.split("::");
		SyncFile syncFile = new SyncFile(split[0], split[1], Boolean.valueOf(split[2]));
		try {
			if (split.length > 3) syncFile.setSize(Long.valueOf(split[3]));
		} catch (NumberFormatException e) {
			log.error("Error parsing file size from composite sync file string {}", composite, e);
		}
		try {
			if (split.length > 4) syncFile.setLastHashed(Long.valueOf(split[4]));
		} catch (NumberFormatException e) {
			log.error("Error parsing last hashed time from composite sync file string {}", composite, e);
		}
		try {
			if (split.length > 5) syncFile.setLastModified(Long.valueOf(split[5]));
		} catch (NumberFormatException e) {
			log.error("Error parsing last modified time from composite sync file string {}", composite, e);
		}
		return syncFile;
	}
}
