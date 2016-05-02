package com.kbdunn.nimbus.web.media;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.MediaGroup;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.bean.MediaGroupBean;
import com.kbdunn.nimbus.web.bean.MediaGroupBeanQuery;
import com.kbdunn.nimbus.web.bean.SongBean;
import com.kbdunn.nimbus.web.bean.SongBeanQuery;
import com.kbdunn.nimbus.web.bean.MediaGroupBeanQuery.MediaGroupBeanQueryController;
import com.kbdunn.nimbus.web.bean.SongBeanQuery.SongBeanQueryController;
import com.kbdunn.nimbus.web.bean.VideoBean;
import com.kbdunn.nimbus.web.bean.VideoBeanQuery;
import com.kbdunn.nimbus.web.bean.VideoBeanQuery.VideoBeanQueryController;
import com.kbdunn.nimbus.web.controlbar.MediaControlBar;
import com.kbdunn.nimbus.web.header.MediaPlayer;
import com.kbdunn.nimbus.web.interfaces.Refreshable;
import com.kbdunn.nimbus.web.media.MediaGroupTable.MediaGroupTableController;
import com.kbdunn.nimbus.web.media.SongTable.SongTableController;
import com.kbdunn.nimbus.web.media.VideoTable.VideoTableController;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;

public class MediaController implements Refreshable, SongBeanQueryController, VideoBeanQueryController, MediaGroupBeanQueryController, 
		SongTableController, VideoTableController, MediaGroupTableController {
	
	private static final Logger log = LogManager.getLogger(MediaController.class.getName());
	
	private MediaLibraryService mediaService;
	private MediaState state;
	private MediaControlBar controlBar;
	private MediaGroup currentGroup; // Artist, Album or Playlist
	private MediaPlayer mediaPlayer;
	private MediaBrowserPanel browserPanel;
	private VideoPlayerPanel videoPanel;
	private SongTable songTable;
	private VideoTable videoTable;
	
	public MediaController() {
		mediaService = NimbusUI.getMediaLibraryService();
		mediaPlayer = NimbusUI.getCurrent().getMediaController();
		
		songTable = new SongTable(this);
		videoTable = new VideoTable(this);
	}
	
	void parseFragment(String fragment) {
		if (fragment == null || fragment.isEmpty()) {
			setState(getDefaultState());
			return;
		}
		MediaBrowserUri uri = new MediaBrowserUri(fragment);
		if (uri.getState() == null) {
			UI.getCurrent().getNavigator().navigateTo(MediaView.NAME);
		} else if (!uri.isValid()) {
			UI.getCurrent().getNavigator().navigateTo(uri.getState().uri().toString());
		}
		setState(uri.getState());
		if (uri.getMediaGroup() != null) 
			setCurrentGroup(uri.getMediaGroup());
	}
	
	@Override
	public void refresh() {
		browserPanel.refreshItemTable();
		browserPanel.refreshGroupTable();
	}
	
	public static MediaState getDefaultState() {
		return MediaState.ARTISTS;
	}
	
	public void setMediaControlBar(MediaControlBar controlBar) {
		this.controlBar = controlBar;
	}
	
	public void setVideoPanel(VideoPlayerPanel videoPanel) {
		this.videoPanel = videoPanel;
	}
	
	public void setBrowserPanel(MediaBrowserPanel browserPanel) {
		this.browserPanel = browserPanel;
	}
	
	public MediaGroupTable getGroupSelector() {
		return browserPanel.getGroupSelectTable();
	}
	
	public Table getItemSelector() {
		return browserPanel.getItemSelectTable();
	}
	
	public void unselectAll() {
		if (state == MediaState.VIDEOS) {
			videoTable.unselectAll();
		} else {
			songTable.unselectAll();
		}
	}
	
	public void select(MediaGroup g) {
		if (state == MediaState.VIDEOS) {
			videoTable.select(g);
		} else {
			songTable.select(g);
		}
	}
	
	public Artist getArtist(String name) {
		return mediaService.getArtistByName(NimbusUI.getCurrentUser(), name);
	}
	
	public Album getAlbum(String artistName, String albumName) {
		return mediaService.getAlbumByName(NimbusUI.getCurrentUser(), artistName, albumName);
	}
	
	public Playlist getPlaylist(String name) {
		return mediaService.getPlaylistByName(NimbusUI.getCurrentUser(), name);
	}
	
	public void setState(MediaState state) {
		if (this.state != null && this.state.equals(state)) return;
		log.debug("Changing state to " + state);
		currentGroup = null;
		this.state = state;
		
		if (state == MediaState.VIDEOS) {
			changeItemSelector(videoTable);
			browserPanel.displayGroupSelector(false);
			videoPanel.setVisible(true);
			controlBar.hidePlaylistControls();
			controlBar.hideEditControls();
		} else {
			changeItemSelector(songTable);
			
			if (state == MediaState.SONGS) browserPanel.displayGroupSelector(false);
			else browserPanel.displayGroupSelector(true);
			
			if (state == MediaState.PLAYLISTS) controlBar.showPlaylistControls();
			else controlBar.hidePlaylistControls();
			
			videoPanel.setVisible(false);
			controlBar.showEditControls();
		}
		refresh();
	}
	
	protected void play(NimbusFile media) {
		if (state != MediaState.VIDEOS)
			mediaPlayer.playFirst(media);
		else {
			videoPanel.play(media);
			getItemSelector().select(media);
		}
	}
	
	private void changeItemSelector(Table newSelector) {
		browserPanel.setItemTable(newSelector);
		browserPanel.refreshItemTable();
		mediaPlayer.refreshCurrentPlaylist();
	}
	
	public MediaState getState() {
		return state == null ? getDefaultState() : state;
	}
	
	public MediaGroup getCurrentGroup() {
		return currentGroup;
	}
	
	public void setCurrentGroup(MediaGroup currentGroup) {
		if (currentGroup == null) {
			this.currentGroup = null;
			browserPanel.refreshItemTable();
			return;
		}
		if (state == MediaState.ARTISTS && !(currentGroup instanceof Artist)) throw new IllegalArgumentException("Current group must be an Artist");
		if (state == MediaState.ALBUMS && !(currentGroup instanceof Album)) throw new IllegalArgumentException("Current group must be an Album");
		if (state == MediaState.PLAYLISTS && !(currentGroup instanceof Playlist)) throw new IllegalArgumentException("Current group must be a Playlist");
		this.currentGroup = currentGroup;
		log.debug("Selecting " + currentGroup);
		getGroupSelector().select(new MediaGroupBean(currentGroup));
		browserPanel.refreshItemTable();
	}
	
	/*public boolean save(Song song) {
		return mediaService.save(song);
	}*/
	
	public boolean save(List<Song> songs) {
		boolean success = true;
		for (Song s : songs) {
			if (!mediaService.save(s)) {
				success = false;
				break;
			}
		}
		refresh();
		return success;
	}
	
	public boolean save(Playlist playlist) {
		return mediaService.save(playlist);
	}
	
	public boolean save(Playlist playlist, List<Song> songs) {
		if (!mediaService.save(playlist)) return false;
		return mediaService.setPlaylistSongs(playlist, songs);
	}
	
	@Override
	public List<Song> getCurrentSongs(SongBeanQuery instance, int startIndex, int count) {
		if (state == MediaState.SONGS) return mediaService.getSongs(NimbusUI.getCurrentUser(), startIndex, count);
		else if (currentGroup instanceof Artist) return mediaService.getArtistSongs(NimbusUI.getCurrentUser(), (Artist) currentGroup, startIndex, count);
		else if (currentGroup instanceof Album) return mediaService.getAlbumSongs(NimbusUI.getCurrentUser(), (Album) currentGroup, startIndex, count);
		else if (currentGroup instanceof Playlist) return mediaService.getPlaylistSongs((Playlist) currentGroup, startIndex, count);
		return Collections.emptyList();
	}
	
	@Override
	public int getCurrentSongCount(SongBeanQuery instance) {
		if (state == MediaState.SONGS) return mediaService.getSongCount(NimbusUI.getCurrentUser());
		else if (currentGroup instanceof Artist) return mediaService.getArtistSongCount(NimbusUI.getCurrentUser(), (Artist) currentGroup);
		else if (currentGroup instanceof Album) return mediaService.getAlbumSongCount(NimbusUI.getCurrentUser(), (Album) currentGroup);
		else if (currentGroup instanceof Playlist) return mediaService.getPlaylistSongCount((Playlist) currentGroup);
		return 0;
	}
	
	/*public List<Video> getVideos() {
		return mediaService.getVideos(NimbusUI.getCurrentUser());
	}*/
	
	@Override
	public List<Video> getCurrentVideos(VideoBeanQuery instance, int startIndex, int count) {
		if (state == MediaState.VIDEOS) return mediaService.getVideos(NimbusUI.getCurrentUser(), startIndex, count);
		return Collections.emptyList();
	}

	@Override
	public int getCurrentVideoCount(VideoBeanQuery instance) {
		if (state == MediaState.VIDEOS) return mediaService.getVideoCount(NimbusUI.getCurrentUser());
		return 0;
	}
	
	public List<NimbusFile> getCurrentMediaFiles() {
		List<?> media = null;
		if (state == MediaState.VIDEOS) media = mediaService.getVideos(NimbusUI.getCurrentUser());
		else if (state == MediaState.SONGS) media = mediaService.getSongs(NimbusUI.getCurrentUser());
		else if (currentGroup instanceof Artist) media = mediaService.getArtistSongs(NimbusUI.getCurrentUser(), (Artist) currentGroup);
		else if (currentGroup instanceof Album) media = mediaService.getAlbumSongs(NimbusUI.getCurrentUser(), (Album) currentGroup);
		else if (currentGroup instanceof Playlist) media = mediaService.getPlaylistSongs((Playlist) currentGroup);
		if (media == null || media.isEmpty())
			return Collections.emptyList();
		List<NimbusFile> files = new ArrayList<NimbusFile>();
		for (Object o : media) {
			files.add((NimbusFile) o);
		}
		return files;
	}
	
	public List<NimbusFile> getSelectedMediaFiles() {
		if (state == MediaState.VIDEOS) return videoTable.getSelected();
		return songTable.getSelected();
	}

	@Override
	public List<MediaGroup> getCurrentMediaGroups(MediaGroupBeanQuery instance, int startIndex, int count) {
		List<?> groups = null;
		if (state == MediaState.ARTISTS) groups = mediaService.getArtists(NimbusUI.getCurrentUser(), startIndex, count);
		else if (state == MediaState.ALBUMS) groups = mediaService.getAlbums(NimbusUI.getCurrentUser(), startIndex, count);
		else if (state == MediaState.PLAYLISTS) groups = mediaService.getPlaylists(NimbusUI.getCurrentUser(), startIndex, count);
		if (groups == null) return Collections.emptyList();
		List<MediaGroup> casted = new ArrayList<MediaGroup>();
		for (Object o : groups) {
			casted.add((MediaGroup) o);
		}
		return casted;
	}

	@Override
	public int getCurrentMediaGroupCount(MediaGroupBeanQuery instance) {
		if (state == MediaState.ARTISTS) return mediaService.getArtistCount(NimbusUI.getCurrentUser());
		else if (state == MediaState.ALBUMS) return mediaService.getAlbumCount(NimbusUI.getCurrentUser());
		else if (state == MediaState.PLAYLISTS) return mediaService.getPlaylistCount(NimbusUI.getCurrentUser());
		return 0;
	}

	@Override
	public MediaState getState(SongTable instance) {
		return getState();
	}

	@Override
	public void handleClick(SongTable instance, SongBean clicked, boolean isDoubleClick) {
		if (isDoubleClick) {
			play(clicked.getSong());
		}
	}

	@Override
	public MediaState getState(VideoTable instance) {
		return getState();
	}

	@Override
	public void handleClick(VideoTable instance, VideoBean clicked, boolean isDoubleClick) {
		if (isDoubleClick) {
			play(clicked.getVideo());
		}
	}

	@Override
	public MediaState getState(MediaGroupTable instance) {
		return getState();
	}

	@Override
	public void handleClick(MediaGroupTable instance, MediaGroupBean clicked, boolean isDoubleClick) {
		if (isDoubleClick) {
			String uri = null;
			MediaGroup g = clicked.getMediaGroup();
			if (g instanceof Artist) uri = new MediaBrowserUri((Artist) g).getUri();
			if (g instanceof Album) uri = new MediaBrowserUri((Album) g).getUri();
			if (g instanceof Playlist) uri = new MediaBrowserUri((Playlist) g).getUri();
			if (uri == null) throw new IllegalStateException("Selected item in GroupSelectTable is not an Artist, Album or Playlist item");
			NimbusUI.getCurrent().getNavigator().navigateTo(uri);
		}
	}
}
