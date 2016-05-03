package com.kbdunn.nimbus.common.util;

import java.io.File;

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
}
