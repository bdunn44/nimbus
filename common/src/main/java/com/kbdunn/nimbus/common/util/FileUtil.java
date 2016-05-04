package com.kbdunn.nimbus.common.util;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
	
	public static File relativize(File base, File child) {
		return new File(base.toURI().relativize(child.toURI()).getPath());
	}
	
	public static void sortPreorder(List<File> unsortedFiles) {
		Collections.sort(unsortedFiles, new Comparator<File>() {
			@Override
			public int compare(File o1, File o2) {
				return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
			}
		});
	}
}
