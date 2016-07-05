package com.kbdunn.nimbus.web.media;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.MediaGroup;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.common.util.StringUtil;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.interfaces.NimbusUri;
import com.kbdunn.nimbus.web.media.MediaState;

public class MediaBrowserUri implements NimbusUri {
	
	private static final Logger log = LogManager.getLogger(MediaBrowserUri.class.getName());

	private MediaLibraryService mediaService;
	private String uri;
	private boolean isValid = false;
	private MediaState state;
	private Artist artist;
	private Album album;
	private Playlist playlist;
	
	public MediaBrowserUri(MediaState state) {
		this.state = state;
		compose();
	}
	
	public MediaBrowserUri(Artist artist) {
		this.state = MediaState.ARTISTS;
		this.artist = artist;
		compose();
	}
	
	public MediaBrowserUri(Album album) {
		this.state = MediaState.ALBUMS;
		this.album = album;
		this.artist = new Artist(album.getArtistName());
		compose();
	}
	
	public MediaBrowserUri(Playlist playlist) {
		this.state = MediaState.PLAYLISTS;
		this.playlist = playlist;
		compose();
	}
	
	public MediaBrowserUri(String uri) {
		this.uri = uri;
		this.mediaService = NimbusUI.getMediaLibraryService();
		parse();
	}
	
	@Override
	public boolean isValid() {
		return isValid;
	}
	
	@Override
	public String getUri() {
		return uri;
	}
	
	public MediaState getState() {
		return state;
	}
	
	public Artist getArtist() {
		return artist;
	}
	
	public Album getAlbum() {
		return album;
	}
	
	public Playlist getPlaylist() {
		return playlist;
	}
	
	public MediaGroup getMediaGroup() {
		if (!isValid) return null;
		if (playlist != null) return playlist;
		if (album != null) return album;
		if (artist != null) return artist;
		return null;
	}
	
	private void compose() {
		uri = MediaView.NAME + "/" + state.lowerValue();
		if (album != null) {
			uri += "/" + artist.getName();
			uri += "/" + album.getName();
		} else if (artist != null) {
			uri += "/" + artist.getName();
		} else if (playlist != null) {
			uri += "/" + playlist.getName();
		}
		isValid = true;
		
		uri = StringUtil.encodeFragmentUtf8(uri);
	}
	
	private void parse() {
		String tmpuri = uri.startsWith("/") ? uri.substring(1) : uri;
		tmpuri = uri.endsWith("/") ? tmpuri : tmpuri + "/";
		if (!tmpuri.startsWith(MediaView.NAME)) {
			isValid = false;
			return;
		}
		tmpuri = tmpuri.substring(MediaView.NAME.length() + 1);
		if (tmpuri.isEmpty()) {
			state = MediaController.getDefaultState();
			isValid = true;
			return;
		}
		String stateString = tmpuri.substring(0, tmpuri.indexOf("/"));
		state = null;
		try {
			state = MediaState.valueOf(stateString.toUpperCase());
		} catch(IllegalArgumentException e) {
			log.warn("Bad media state value in URI: " + stateString);
			isValid = false;
			return;
		}
		tmpuri = tmpuri.substring(state.lowerValue().length() + 1);
		if (state == MediaState.SONGS || state == MediaState.VIDEOS || tmpuri.isEmpty()) {
			isValid = true;
			return;
		}
		
		String first = tmpuri.substring(0, tmpuri.indexOf("/"));
		first = StringUtil.decodeUtf8(first);
		if (state == MediaState.ARTISTS || state == MediaState.ALBUMS) {
			artist = mediaService.getArtistByName(NimbusUI.getCurrentUser(), first);
			if (artist == null) {
				isValid = false;
				return;
			}
		} else if (state == MediaState.PLAYLISTS) {
			playlist = mediaService.getPlaylistByName(NimbusUI.getCurrentUser(), first);
			if (playlist == null) {
				isValid = false;
				return;
			}
		} 
		
		if (state == MediaState.ALBUMS) {
			tmpuri = tmpuri.substring(first.length() + 1);
			if (tmpuri.isEmpty()) {
				isValid = false;
				return;
			}
			String second = tmpuri.substring(0, tmpuri.indexOf("/"));
			second = StringUtil.decodeUtf8(second);
			album = mediaService.getAlbumByName(NimbusUI.getCurrentUser(), artist.getName(), second);
			if (album == null) {
				isValid = false;
				return;
			}
		}
		isValid = true;
	}
	
	@Override
	public String toString() {
		return getUri();
	}
}
