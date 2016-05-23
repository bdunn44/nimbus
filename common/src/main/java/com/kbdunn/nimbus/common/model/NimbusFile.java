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
	private Boolean song;
	private Boolean video;
	private Boolean image;
	private Boolean directory;
	private Boolean reconciled;
	private Long lastReconciled;
	private Boolean libraryRemoved;
	private String md5;
	private Long lastHashed;
	private Date created;
	private Date updated;
	
	/* DAO constructor to instantiate a persisted NimbusFile */
	public NimbusFile(Long id, Long userId, Long storageDeviceId, String path, Boolean directory, Long size, Boolean song, Boolean video, Boolean image, 
			Boolean reconciled, Long lastReconciled, Boolean libraryRemoved, String md5, Long lastHashed, Date createDate, Date lastUpdateDate) {
		this.id = id;
		this.userId = userId;
		this.storageDeviceId = storageDeviceId;
		this.path = path;
		this.directory = directory;
		this.size = size;
		this.song = song;
		this.video = video;
		this.image = image;
		this.reconciled = reconciled;
		this.lastReconciled = lastReconciled;
		this.libraryRemoved = libraryRemoved;
		this.md5 = md5;
		this.lastHashed = lastHashed;
		this.created = createDate;
		this.updated = lastUpdateDate;
	}
	
	/* Used by sub-classes to convert a NimbusFile into a Song/Video/.. */
	protected NimbusFile(NimbusFile copy) {
		this.id = copy.getId();
		this.userId = copy.getUserId();
		this.storageDeviceId = copy.getStorageDeviceId();
		this.path = copy.getPath();
		this.directory = copy.isDirectory();
		this.size = copy.getSize();
		this.song = copy.isSong();
		this.video = copy.isVideo();
		this.image = copy.isImage();
		this.reconciled = copy.isReconciled();
		this.lastReconciled = copy.getLastReconciled();
		this.libraryRemoved = copy.isLibraryRemoved();
		this.md5 = copy.getMd5();
		this.lastHashed = copy.getLastHashed();
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
		return directory != null && directory;
	}
	
	public void setDirectory(Boolean isDirectory) {
		this.directory = isDirectory;
	}
	
	public boolean isReconciled() {
		return reconciled != null && reconciled;
	}
	
	public void setReconciled(Boolean isReconciled) {
		this.reconciled = isReconciled;
	}
	
	public Long getLastReconciled() {
		return lastReconciled;
	}

	public void setLastReconciled(Long lastReconciled) {
		this.lastReconciled = lastReconciled;
	}
	
	public boolean isSong() {
		return song != null && song;
	}
	
	public void setSong(Boolean isSong) {
		this.song = isSong;
	}
	
	public boolean isVideo() {
		return video != null && video;
	}
	
	public void setVideo(Boolean isVideo) {
		this.video = isVideo;
	}
	
	public boolean isImage() {
		return image != null && image;
	}
	
	public void setImage(Boolean isImage) {
		this.image = isImage;
	}
	
	public boolean isLibraryRemoved() {
		return libraryRemoved != null && libraryRemoved;
	}
	
	public void setIsLibraryRemoved(Boolean isLibraryRemoved) {
		this.libraryRemoved = isLibraryRemoved;
	}
	
	public String getMd5() {
		return md5;
	}
	
	public void setMd5(String md5) {
		this.md5 = md5;
	}
	
	public Long getLastHashed() {
		return lastHashed;
	}
	
	public void setLastHashed(Long lastHashed) {
		this.lastHashed = lastHashed;
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
				+ storageDeviceId + ", path=" + path + ", size=" + size + ", song="
				+ song + ", video=" + video + ", image=" + image
				+ ", directory=" + directory + ", reconciled="
				+ reconciled + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((directory == null) ? 0 : directory.hashCode());
		result = prime * result + ((image == null) ? 0 : image.hashCode());
		result = prime * result + ((song == null) ? 0 : song.hashCode());
		result = prime * result + ((video == null) ? 0 : video.hashCode());
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
		if (directory == null) {
			if (other.directory != null)
				return false;
		} else if (!directory.equals(other.directory))
			return false;
		if (image == null) {
			if (other.image != null)
				return false;
		} else if (!image.equals(other.image))
			return false;
		if (song == null) {
			if (other.song != null)
				return false;
		} else if (!song.equals(other.song))
			return false;
		if (video == null) {
			if (other.video != null)
				return false;
		} else if (!video.equals(other.video))
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
		int c = ComparatorUtil.nullSafeBooleanComparator(o.directory, this.directory);
		c = c == 0 ? ComparatorUtil.nullSafeStringComparator(this.getName(), o.getName()) : c;
		return c;
	}
}