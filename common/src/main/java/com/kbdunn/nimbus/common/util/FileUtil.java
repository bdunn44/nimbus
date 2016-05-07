package com.kbdunn.nimbus.common.util;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kbdunn.nimbus.common.sync.HashUtil;
import com.kbdunn.nimbus.common.sync.model.SyncFile;

public class FileUtil {

	public static final String INVALID_FILENAME_CHARACTERS = "/\\?%*:|\"<>";
	public static final String INVALID_PATH_CHARACTERS = "\\?%*:|\"<>";
	
	private FileUtil() {
		// Only static methods
	}
	
	public static boolean pathContainsInvalidCharacters(String path) {
		return StringUtil.stringContainsInvalidCharacters(path, INVALID_PATH_CHARACTERS);
	}
	
	public static boolean filenameContainsInvalidCharacters(String name) {
		return StringUtil.stringContainsInvalidCharacters(name, INVALID_FILENAME_CHARACTERS);
	}
	
	/*public static File relativize(File base, File child) {
		return new File(base.toURI().relativize(child.toURI()).getPath());
	}*/
	
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
	
	public static SyncFile toSyncFile(File syncRootDir, File file, boolean isDirectory) throws IOException {
		return new SyncFile(
				file.getAbsolutePath().replace(syncRootDir.getAbsolutePath(), "").replace("\\", "/"), 
				isDirectory ? HashUtil.hash(file) : null,
				isDirectory
			);
	}
	
	/**
	 * Returns a list of all nodes in preorder. One can specify whether only files, folders or both is desired
	 */
	/*public static List<SyncFile> getFlatNodeList(SyncFile root, boolean addFiles, boolean addFolders) {
		if (!addFiles && !addFolders) {
			throw new IllegalArgumentException("Must visit either files, folders or both");
		}
		List<SyncFile> set = new LinkedList<>();
		flattenNode(root, set, addFiles, addFolders);
		return set;
	}
	
	private static void flattenNode(SyncFile current, List<SyncFile> list, boolean addFiles, boolean addFolders) {
		if (current == null) {
			return;
		}
		
		if (current.isFile() && addFiles || current.isFolder() && addFolders) {
			list.add(current);
		}
		
		if (current.isFolder()) {
			for (SyncFile child : current.getChildren()) {
				flattenNode(child, list, addFiles, addFolders);
			}
		}
	}*/
}
