package com.kbdunn.nimbus.api.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.util.FileUtil;

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
	
	public static SyncFile toSyncFile(NimbusFile syncRootDir, NimbusFile file) throws IOException {
		String path = file.getPath().replace(syncRootDir.getPath(), "").replace("\\", "/");
		return new SyncFile(
			file.getPath().replace(syncRootDir.getPath(), "").replace("\\", "/"), 
			file.isDirectory() ? "" : file.getMd5(),
			file.isDirectory(),
			file.getSize() == null && Files.exists(Paths.get(path)) ? FileUtil.getFileSize(file) : file.getSize(),
			file.getLastHashed(),
			file.getLastModified() == null && Files.exists(Paths.get(path)) ? FileUtil.getLastModifiedTime(file) : file.getLastModified()
		);
	}
	
	public static SyncFile toSyncFile(File syncRootDir, File file, boolean isDirectory) throws IOException {
		return toSyncFile(syncRootDir, file, isDirectory, null, 0);
	}
	
	public static SyncFile toSyncFile(File syncRootDir, File file, boolean isDirectory, String md5, long lastHashed) throws IOException {
		if (!isDirectory) {
			// Ensure directories don't have a hash
			md5 = "";
		}
		return new SyncFile(
				getRelativeSyncFilePath(syncRootDir, file), 
				md5,
				isDirectory,
				Files.exists(file.toPath()) ? Files.size(file.toPath()) : 0,
				lastHashed,
				Files.exists(file.toPath()) ? Files.getLastModifiedTime(file.toPath()).toMillis() : 0
			);
	}
	
	public static String getRelativeSyncFilePath(File syncRootDir, File file) {
		return file.getAbsolutePath().replace(syncRootDir.getAbsolutePath(), "").replace("\\", "/");
	}
}
