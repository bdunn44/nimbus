package com.kbdunn.nimbus.common.sync.model;

import java.io.File;

import com.kbdunn.nimbus.common.util.StringUtil;

public class SyncFile {

	private final String path;
	private final File file;
	private final byte[] md5;
	private final boolean isDirectory;

	public SyncFile(String path, byte[] md5, boolean isDirectory) {
		this.path = path;
		this.file = new File(path);
		this.md5 = md5;
		this.isDirectory = isDirectory;
	}
	
	public String getName() {
		return file.getName();
	}

	public String getPath() {
		return path;
	}

	public boolean isFile() {
		return !isDirectory;
		/*if (file.exists()) {
			return file.isFile();
		} else {
			return md5 != null;
		}*/
	}

	public boolean isDirectory() {
		return isDirectory;
	}

	public byte[] getMd5() {
		return md5;
	}

	@Override
	public String toString() {
		return String.format("%s: %s [%s]", isFile() ? "File" : "Folder", getPath(), 
				isFile() ? String.format("(MD5: %s)", StringUtil.bytesToHex(getMd5())) : "");
	}
}
