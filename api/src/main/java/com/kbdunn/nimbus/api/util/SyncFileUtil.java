package com.kbdunn.nimbus.api.util;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.common.model.NimbusFile;

public class SyncFileUtil {
	
	private SyncFileUtil() {  }
	
	public static void sortPreorder(List<SyncFile> unsortedFiles) {
		Collections.sort(unsortedFiles, new Comparator<SyncFile>() {
			@Override
			public int compare(SyncFile o1, SyncFile o2) {
				if (o1.getPath().equals(o2.getPath())) return 0;
				if (o1.getPath().startsWith(o2.getPath())) return 1;
				if (o2.getPath().startsWith(o1.getPath())) return -1;
				return 0;
			}
		});
	}
	
	public static SyncFile toSyncFile(NimbusFile syncRootDir, NimbusFile file) {
		return new SyncFile(
			file.getPath().replace(syncRootDir.getPath(), "").replace("\\", "/"), 
			file.getMd5(),
			file.isDirectory()
		);
	}
	
	public static SyncFile toSyncFile(File syncRootDir, File file, boolean isDirectory) {
		return toSyncFile(syncRootDir, file, isDirectory, null);
	}
	
	public static SyncFile toSyncFile(File syncRootDir, File file, boolean isDirectory, String md5) {
		if (!isDirectory) {
			// Ensure directories don't have a hash
			md5 = "";
		}
		return new SyncFile(
				file.getAbsolutePath().replace(syncRootDir.getAbsolutePath(), "").replace("\\", "/"), 
				md5,
				isDirectory
			);
	}
}
