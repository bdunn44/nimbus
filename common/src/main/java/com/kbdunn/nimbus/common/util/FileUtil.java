package com.kbdunn.nimbus.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Date;

import com.kbdunn.nimbus.common.model.NimbusFile;

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
	
	public static long getLastModifiedTime(NimbusFile file) throws IOException {
		return Files.getLastModifiedTime(Paths.get(file.getPath()), LinkOption.NOFOLLOW_LINKS).toMillis();
	}
	
	public static Date getLastModifiedDate(NimbusFile file) throws IOException {
		return new Date(getLastModifiedTime(file));
	}
	
	public static long getFileSize(NimbusFile file) throws IOException {
		return Files.size(Paths.get(file.getPath()));
	}
	
	/*public static File relativize(File base, File child) {
		return new File(base.toURI().relativize(child.toURI()).getPath());
	}*/
}
