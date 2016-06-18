package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class Song extends NimbusFile {

	private String title;
	private Integer length;
	private Integer trackNumber;
	private String artist;
	private String album;
	private String albumYear;
	
	public Song(Long id, Long userId, Long driveId, String path, Boolean isDirectory, Long size, 
			Boolean isSong, Boolean isVideo, Boolean isImage, Boolean isReconciled, Long lastReconciled, Boolean isLibraryRemoved, 
			String md5, Long lastHashed, Long lastModified, Date createDate, Date lastUpdateDate, String title, Integer length, Integer trackNumber, String artist, 
			String album, String albumYear) {
		super(id, userId, driveId, path, isDirectory, size, isSong, isVideo, isImage,
				isReconciled, lastReconciled, isLibraryRemoved, md5, lastHashed, lastModified, createDate, lastUpdateDate);
		this.title = title;
		this.length = length;
		this.trackNumber = trackNumber;
		this.artist = artist;
		this.album = album;
		this.albumYear = albumYear;
	}
	
	public Song(Song song) {
		super(song);
		this.title = song.title;
		this.length = song.length;
		this.trackNumber = song.trackNumber;
		this.artist = song.artist;
		this.album = song.album;
		this.albumYear = song.albumYear;
	}
	
	public Song(NimbusFile nf) {
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
	
	public Integer getTrackNumber() {
		return trackNumber;
	}
	
	public void setTrackNumber(Integer trackNumber) {
		this.trackNumber = trackNumber;
	}
	
	public String getArtist() {
		return artist == null || artist.isEmpty() ? Artist.UNKNOWN : artist;
	}
	
	public void setArtist(String artist) {
		this.artist = artist;
	}
	
	public String getAlbum() {
		return album == null || album.isEmpty() ? Album.UNKNOWN : album;
	}
	
	public void setAlbum(String album) {
		this.album = album;
	}
	
	public String getAlbumYear() {
		return albumYear;
	}
	
	public void setAlbumYear(String albumYear) {
		this.albumYear = albumYear;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((getAlbum() == null) ? 0 : getAlbum().hashCode());
		result = prime * result
				+ ((albumYear == null) ? 0 : albumYear.hashCode());
		result = prime * result + ((getArtist() == null) ? 0 : getArtist().hashCode());
		result = prime * result + ((length == null) ? 0 : length.hashCode());
		result = prime * result + ((getTitle() == null) ? 0 : getTitle().hashCode());
		result = prime * result
				+ ((trackNumber == null) ? 0 : trackNumber.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof Song))
			return false;
		Song other = (Song) obj;
		if (getAlbum() == null) {
			if (other.getAlbum() != null)
				return false;
		} else if (!getAlbum().equals(other.getAlbum()))
			return false;
		if (albumYear == null) {
			if (other.albumYear != null)
				return false;
		} else if (!albumYear.equals(other.albumYear))
			return false;
		if (getArtist() == null) {
			if (other.artist != null)
				return false;
		} else if (!getArtist().equals(other.getArtist()))
			return false;
		if (length == null) {
			if (other.length != null)
				return false;
		} else if (!length.equals(other.length))
			return false;
		if (getTitle() == null) {
			if (other.getTitle() != null)
				return false;
		} else if (!getTitle().equals(other.getTitle()))
			return false;
		if (trackNumber == null) {
			if (other.trackNumber != null)
				return false;
		} else if (!trackNumber.equals(other.trackNumber))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Song [title=" + getTitle() + ", length=" + length + ", trackNumber="
				+ trackNumber + ", artist=" + getArtist() + ", album=" + getAlbum()
				+ ", albumYear=" + albumYear + ", path=" + getPath() + "]";
	}
}
