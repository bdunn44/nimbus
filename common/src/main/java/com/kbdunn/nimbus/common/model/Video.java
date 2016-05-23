package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class Video extends NimbusFile {

	protected String title;
	protected Integer length;
	
	public Video(Long id, Long userId, Long driveId, String path, Boolean isDirectory, Long size,  Boolean isSong, 
			Boolean isVideo, Boolean isImage, Boolean isReconciled, Long lastReconciled, Boolean isLibraryRemoved, 
			String md5, Long lastHashed, Date createDate, Date lastUpdateDate, String title, Integer length) {
		super(id, userId, driveId, path, isDirectory, size, isSong, isVideo, isImage,
				isReconciled, lastReconciled, isLibraryRemoved, md5, lastHashed, createDate, lastUpdateDate);
		this.title = title;
		this.length = length;
	}
	
	public Video(NimbusFile nf) {
		super(nf);
	}
	
	public String getTitle() {
		return title == null || title.isEmpty() ? getName() : title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public Integer getLength() {
		return length;
	}
	
	public void setLength(Integer length) {
		this.length = length;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((length == null) ? 0 : length.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Video))
			return false;
		Video other = (Video) obj;
		if (length == null) {
			if (other.length != null)
				return false;
		} else if (!length.equals(other.length))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Video [title=" + title + ", length=" + length + ", path=" + getPath() + "]";
	}
}