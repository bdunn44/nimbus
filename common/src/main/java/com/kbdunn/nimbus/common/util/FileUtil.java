package com.kbdunn.nimbus.common.util;

public class FileUtil {

	public static final String INVALID_FILENAME_CHARACTERS = "/\\?%*:|\"<>";
	public static final String INVALID_PATH_CHARACTERS = "\\?%*:|\"<>";
	
	public static boolean pathContainsInvalidCharacters(String path) {
		return StringUtil.stringContainsInvalidCharacters(path, INVALID_PATH_CHARACTERS);
	}
	
	public static boolean filenameContainsInvalidCharacters(String name) {
		return StringUtil.stringContainsInvalidCharacters(name, INVALID_FILENAME_CHARACTERS);
	}
}
