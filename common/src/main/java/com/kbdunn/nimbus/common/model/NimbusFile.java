package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.kbdunn.nimbus.common.util.ComparatorUtil;
import com.kbdunn.nimbus.common.util.StringUtil;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=As.PROPERTY)
public class NimbusFile implements NimbusRecord, FileContainer, Comparable<NimbusFile> {

	private Long id;
	private Long userId;
	private Long storageDeviceId;
	private String path;
	private Long size;
	private Boolean isSong;
	private Boolean isVideo;
	private Boolean isImage;
	private Boolean isDirectory;
	private Boolean isReconciled;
	private Long lastReconciled;
	private Boolean isLibraryRemoved;
	private Date created;
	private Date updated;
	
	/* DAO constructor to instantiate a persisted NimbusFile */
	public NimbusFile(Long id, Long userId, Long storageDeviceId, String path, Boolean isDirectory, Long size, Boolean isSong, Boolean isVideo, Boolean isImage, 
			Boolean isReconciled, Long lastReconciled, Boolean isLibraryRemoved, Date createDate, Date lastUpdateDate) {
		this.id = id;
		this.userId = userId;
		this.storageDeviceId = storageDeviceId;
		this.path = path;
		this.isDirectory = isDirectory;
		this.size = size;
		this.isSong = isSong;
		this.isVideo = isVideo;
		this.isImage = isImage;
		this.isReconciled = isReconciled;
		this.lastReconciled = lastReconciled;
		this.isLibraryRemoved = isLibraryRemoved;
		this.created = createDate;
		this.updated = lastUpdateDate;
	}
	
	/* Used by sub-classes to convert a NimbusFile into a Song/Video/.. */
	protected NimbusFile(NimbusFile copy) {
		this.id = copy.getId();
		this.userId = copy.getUserId();
		this.storageDeviceId = copy.getStorageDeviceId();
		this.path = copy.getPath();
		this.isDirectory = copy.isDirectory();
		this.size = copy.getSize();
		this.isSong = copy.isSong();
		this.isVideo = copy.isVideo();
		this.isImage = copy.isImage();
		this.isReconciled = copy.isReconciled();
		this.lastReconciled = copy.getLastReconciled();
		this.isLibraryRemoved = copy.isLibraryRemoved();
		this.created = copy.getCreated();
		this.updated = copy.getUpdated();
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getStorageDeviceId() {
		return storageDeviceId;
	}
	
	public void setStorageDeviceId(Long storageDeviceId) {
		this.storageDeviceId = storageDeviceId;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getFileExtension() {
		return StringUtil.getFileExtensionFromPath(getPath());
	}
	
	public Long getSize() {
		return size;
	}
	
	public void setSize(Long size) {
		this.size = size;
	}
	
	public boolean isDirectory() {
		return isDirectory != null && isDirectory;
	}
	
	public void setDirectory(Boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
	
	public boolean isReconciled() {
		return isReconciled != null && isReconciled;
	}
	
	public void setReconciled(Boolean isReconciled) {
		this.isReconciled = isReconciled;
	}
	
	public Long getLastReconciled() {
		return lastReconciled;
	}

	public void setLastReconciled(Long lastReconciled) {
		this.lastReconciled = lastReconciled;
	}
	
	public boolean isSong() {
		return isSong != null && isSong;
	}
	
	public void setSong(Boolean isSong) {
		this.isSong = isSong;
	}
	
	public boolean isVideo() {
		return isVideo != null && isVideo;
	}
	
	public void setVideo(Boolean isVideo) {
		this.isVideo = isVideo;
	}
	
	public boolean isImage() {
		return isImage != null && isImage;
	}
	
	public void setImage(Boolean isImage) {
		this.isImage = isImage;
	}
	
	public boolean isLibraryRemoved() {
		return isLibraryRemoved != null && isLibraryRemoved;
	}
	
	public void setIsLibraryRemoved(Boolean isLibraryRemoved) {
		this.isLibraryRemoved = isLibraryRemoved;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public void setCreated(Date createDate) {
		this.created = createDate;
	}

	@Override
	public Date getUpdated() {
		return updated;
	}

	@Override
	public void setUpdated(Date lastUpdateDate) {
		this.updated = lastUpdateDate;
	}

	@Override
	public String getName() {
		return StringUtil.getFileNameFromPath(getPath());
	}

	@Override
	public String toString() {
		return "NimbusFile [id=" + id + ", userId=" + userId + ", driveId="
				+ storageDeviceId + ", path=" + path + ", size=" + size + ", isSong="
				+ isSong + ", isVideo=" + isVideo + ", isImage=" + isImage
				+ ", isDirectory=" + isDirectory + ", isReconciled="
				+ isReconciled + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((isDirectory == null) ? 0 : isDirectory.hashCode());
		result = prime * result + ((isImage == null) ? 0 : isImage.hashCode());
		result = prime * result + ((isSong == null) ? 0 : isSong.hashCode());
		result = prime * result + ((isVideo == null) ? 0 : isVideo.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((size == null) ? 0 : size.hashCode());
		result = prime * result
				+ ((storageDeviceId == null) ? 0 : storageDeviceId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof NimbusFile))
			return false;
		NimbusFile other = (NimbusFile) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isDirectory == null) {
			if (other.isDirectory != null)
				return false;
		} else if (!isDirectory.equals(other.isDirectory))
			return false;
		if (isImage == null) {
			if (other.isImage != null)
				return false;
		} else if (!isImage.equals(other.isImage))
			return false;
		if (isSong == null) {
			if (other.isSong != null)
				return false;
		} else if (!isSong.equals(other.isSong))
			return false;
		if (isVideo == null) {
			if (other.isVideo != null)
				return false;
		} else if (!isVideo.equals(other.isVideo))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (size == null) {
			if (other.size != null)
				return false;
		} else if (!size.equals(other.size))
			return false;
		if (storageDeviceId == null) {
			if (other.storageDeviceId != null)
				return false;
		} else if (!storageDeviceId.equals(other.storageDeviceId))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}
	
	@Override
	public int compareTo(NimbusFile o) {
		int c = ComparatorUtil.nullSafeBooleanComparator(o.isDirectory, this.isDirectory);
		c = c == 0 ? ComparatorUtil.nullSafeStringComparator(this.getName(), o.getName()) : c;
		return c;
	}
}