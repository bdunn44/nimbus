package com.kbdunn.nimbus.web.media;

import org.apache.commons.lang.WordUtils;

public enum MediaState {
	ARTISTS(), ALBUMS(), SONGS(), PLAYLISTS(), VIDEOS();
	
	private MediaBrowserUri uri;
	private String value, display;
	
	MediaState() {
		value = this.toString().toLowerCase();
		display = WordUtils.capitalizeFully(this.toString());
		uri = new MediaBrowserUri(this);
	}
	
	public String lowerValue() {
		return value;
	}
	
	public String display() {
		return display;
	}
	
	public MediaBrowserUri uri() {
		return uri;
	}
}