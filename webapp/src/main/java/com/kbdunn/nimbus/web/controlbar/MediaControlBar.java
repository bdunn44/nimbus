package com.kbdunn.nimbus.web.controlbar;

import com.kbdunn.nimbus.web.media.MediaBrowserUri;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.media.MediaState;
import com.kbdunn.nimbus.web.media.action.AddToLibrary;
import com.kbdunn.nimbus.web.media.action.CreatePlaylist;
import com.kbdunn.nimbus.web.media.action.DeletePlaylist;
import com.kbdunn.nimbus.web.media.action.EditMetadata;
import com.kbdunn.nimbus.web.media.action.EditPlaylist;
import com.kbdunn.nimbus.web.media.action.RemoveFromLibrary;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;

public class MediaControlBar extends ControlBar {
	
	private static final long serialVersionUID = 1L;
	
	private MediaController controller;
	private Panel playlistControls;
	private Panel editControls;
	
	public MediaControlBar(MediaController controller) {
		this.controller = controller;
		super.setCaptionsHidden(false);
		addGroupSelectButtons();
		addEditControls();
		addPlaylistControls();
	}
	
	private void addGroupSelectButtons() {
		
		// Songs
		final Button songs = new Button(MediaState.SONGS.display(), FontAwesome.MUSIC);
		songs.setDescription(MediaState.SONGS.display());
		songs.addClickListener(getNavigatorClickListener(MediaState.SONGS.uri()));
		
		// Artists
		final Button artists = new Button(MediaState.ARTISTS.display(), FontAwesome.USERS);
		artists.setDescription(MediaState.ARTISTS.display());
		artists.addClickListener(getNavigatorClickListener(MediaState.ARTISTS.uri()));
		
		// Albums
		final Button albums = new Button(MediaState.ALBUMS.display(), FontAwesome.CARET_SQUARE_O_RIGHT);
		albums.setDescription(MediaState.ALBUMS.display());
		albums.addClickListener(getNavigatorClickListener(MediaState.ALBUMS.uri()));
		
		// Playlists
		final Button playlists = new Button(MediaState.PLAYLISTS.display(), FontAwesome.LIST_OL);
		playlists.setDescription(MediaState.PLAYLISTS.display());
		playlists.addClickListener(getNavigatorClickListener(MediaState.PLAYLISTS.uri()));
		
		// Videos
		final Button videos = new Button(MediaState.VIDEOS.display(), FontAwesome.VIDEO_CAMERA);
		videos.setDescription(MediaState.VIDEOS.display());
		videos.addClickListener(getNavigatorClickListener(MediaState.VIDEOS.uri()));
		
		addControlGroup("Browse", new Button[]{ songs, artists, albums, playlists, videos });
	}
	
	private void addEditControls() {
		final AddToLibrary addAction = new AddToLibrary(controller);
		final Button add = new Button(FontAwesome.PLUS_CIRCLE);
		add.setDescription(AddToLibrary.CAPTION);
		add.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				addAction.showDialog();
			}
		});
		
		final EditMetadata editAction = new EditMetadata(controller);
		final Button edit = new Button(FontAwesome.EDIT);
		edit.setDescription("Edit Songs");
		edit.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				editAction.showDialog();
			}
		});
		
		final RemoveFromLibrary removeAction = new RemoveFromLibrary(controller);
		final Button remove = new Button(FontAwesome.MINUS_CIRCLE);
		remove.setDescription(RemoveFromLibrary.CAPTION);
		remove.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				removeAction.showDialog();
			}
		});
		
		editControls = addControlGroup("Manage", edit, add, remove);
	}
	
	private void addPlaylistControls() {
		// Create Playlist
		final CreatePlaylist createPlaylistDialog = new CreatePlaylist(controller);
		final Button createPlaylist = new Button(FontAwesome.PLUS_SQUARE_O);
		createPlaylist.setDescription("Create Playlist");
		createPlaylist.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				createPlaylistDialog.showDialog();
			}
		});
		
		// Edit Playlist
		final EditPlaylist editPlaylistDialog = new EditPlaylist(controller);
		final Button editPlaylist = new Button(FontAwesome.EDIT);
		editPlaylist.setDescription("Edit Playlist");
		editPlaylist.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				editPlaylistDialog.showDialog();
			}
		});
		
		// Delete Playlist
		final DeletePlaylist deletePlaylistAction = new DeletePlaylist(controller);
		final Button deletePlaylist = new Button(FontAwesome.TRASH);
		deletePlaylist.setDescription("Delete Playlist");
		deletePlaylist.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				deletePlaylistAction.showDialog();
			}
		});
		
		playlistControls = addControlGroup("Playlists", new Button[]{ createPlaylist, editPlaylist, deletePlaylist });
	}
	
	private ClickListener getNavigatorClickListener(final MediaBrowserUri uri) {
		final String navigationState = uri.toString();
		return new ClickListener() {
			private static final long serialVersionUID = 1L;
			
			@Override
			public void buttonClick(ClickEvent event) {
				UI.getCurrent().getNavigator().navigateTo(navigationState);
			}
		};
	}
	
	public void showPlaylistControls() {
		playlistControls.setVisible(true);
	}
	
	public void hidePlaylistControls() {
		playlistControls.setVisible(false);
	}
	
	public void showEditControls() {
		editControls.setVisible(true);
	}
	
	public void hideEditControls() {
		editControls.setVisible(false);
	}
}
