package com.kbdunn.nimbus.web.bean;

import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.util.StringUtil;

public class SongBean {
	
	public static final String PROPERTY_ITEM_ID = "itemId";
	public static final String PROPERTY_ARTIST = "artist";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_LENGTH = "lengthString";
	public static final String PROPERTY_ALBUM = "album";
	public static final String PROPERTY_ALBUM_YEAR = "year";
	public static final String PROPERTY_TRACK_NO = "trackNo";
	
	private Song song;
	private Integer playlistOrder;
	
	public SongBean() {  }
	
	public SongBean(Song song) {
		this.song = song;
	}
	
	public Song getSong() {
		return song;
	}
	
	public Integer getPlaylistOrder() {
		return playlistOrder;
	}
	
	public void setPlaylistOrder(Integer playlistOrder) {
		this.playlistOrder = playlistOrder;
	}
	
	public Object getItemId() {
		return this;
	}
	
	//public void setItemId(Object o) {  }
	
	public Long getId() {
		return song.getId();
	}
	
	//public void setId(Long id) {  }
	
	public String getArtist() {
		return song.getArtist();
	}
	
	//public void setArtist(String artist) {  }
	
	public String getTitle() {
		return song.getTitle();
	}
	
	//public void setTitle(String title) {  }
	
	public String getLengthString() {
		return StringUtil.toDurationString(song.getLength());
	}
	
	//public void setLengthString(String length) {  }
	
	public String getAlbum() {
		return song.getAlbum();
	}
	
	//public void setAlbum(String album) {  }
	
	public String getYear() {
		return song.getAlbumYear();
	}
	
	//public void setYear(String year) {  }
	
	public Integer getTrackNo() {
		return song.getTrackNumber();
	}
	
	//public void setTrackNo(Integer trackNo) {  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((playlistOrder == null) ? 0 : playlistOrder.hashCode());
		result = prime * result + ((song == null) ? 0 : song.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SongBean))
			return false;
		SongBean other = (SongBean) obj;
		if (playlistOrder == null) {
			if (other.playlistOrder != null)
				return false;
		} else if (!playlistOrder.equals(other.playlistOrder))
			return false;
		if (song == null) {
			if (other.song != null)
				return false;
		} else if (!song.equals(other.song))
			return false;
		return true;
	}
}