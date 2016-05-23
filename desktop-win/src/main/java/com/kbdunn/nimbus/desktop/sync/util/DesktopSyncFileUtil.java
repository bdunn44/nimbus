package com.kbdunn.nimbus.desktop.sync.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.desktop.sync.SyncPreferences;

public final class DesktopSyncFileUtil {
	
	private DesktopSyncFileUtil() {  }
	
	public static File toFile(SyncFile syncFile) {
		File syncDir = SyncPreferences.getSyncDirectory();
		if (!syncDir.isDirectory()) throw new IllegalStateException("Sync root directory does not exist: " + syncDir);
		return new File(syncDir, syncFile.getPath());
	}
	
	public static SyncFile toSyncFile(File file, String md5) {
		File syncRootDir = SyncPreferences.getSyncDirectory();
		if (!syncRootDir.isDirectory()) throw new IllegalStateException("Sync root directory does not exist: " + syncRootDir);
		return new SyncFile(
				file.getAbsolutePath().replace(syncRootDir.getAbsolutePath(), "").replace("\\", "/"), 
				md5,
				file.isDirectory()
			);
	}
	
	public static Map<String, SyncFile> buildMap(List<SyncFile> syncFiles) {
		HashMap<String, SyncFile> map = new HashMap<>();
		for (SyncFile syncFile : syncFiles) {
			map.put(syncFile.getPath(), syncFile);
		}
		return map;
	}
}
