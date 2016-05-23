package com.kbdunn.nimbus.api.client.model;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SyncFile {

	private String path;
	private File file;
	private String md5;
	private Boolean directory;

	public SyncFile() {  }
	
	public SyncFile(String path, String md5, boolean directory) {
		this.path = path;
		this.file = new File(path);
		this.md5 = md5;
		this.directory = directory;
	}
	
	@JsonIgnore
	public String getName() {
		return file.getName();
	}

	public String getPath() {
		return path;
	}

	@JsonIgnore
	public boolean isFile() {
		return !directory;
		/*if (file.exists()) {
			return file.isFile();
		} else {
			return md5 != null;
		}*/
	}

	public boolean isDirectory() {
		return directory;
	}

	public String getMd5() {
		return md5;
	}
	
	public void setMd5(String md5) {
		this.md5 = md5;
	}

	@Override
	public String toString() {
		return String.format("%s: %s [%s]", isDirectory() ? "Folder" : "File", getPath(), 
				isDirectory() ? "" : String.format("(MD5: %s)", md5));
	}
}
