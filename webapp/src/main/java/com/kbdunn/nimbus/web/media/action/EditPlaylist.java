package com.kbdunn.nimbus.web.media.action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.MediaGroup;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.bean.SongBean;
import com.kbdunn.nimbus.web.bean.SongBeanQuery;
import com.kbdunn.nimbus.web.bean.SongBeanQuery.SongBeanQueryController;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.media.MediaState;
import com.kbdunn.nimbus.web.media.SongTable;
import com.kbdunn.nimbus.web.media.SongTable.SongTableController;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.Align;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditPlaylist extends VerticalLayout implements ClickListener, SongBeanQueryController, SongTableController {

	private static final long serialVersionUID = -6020470420755012884L;
	
	private NimbusUser user;
	private MediaLibraryService mediaService;
	private MediaController controller;
	private MediaState state;
	private Playlist selected;
	private TextField newName;
	private ComboBox groupSelect;
	private ComboBox groupFilter;
	private SongTable candidates;
	//private LazyQueryContainer cc;
	private Table playlist;
	private BeanItemContainer<SongBean> pc = new BeanItemContainer<SongBean>(SongBean.class);
	//private List<Song> currentCandidates;
	private MediaGroup currentGroup;
	private PopupWindow popup;
	private static final String caption = "Edit Playlist";
	private boolean layoutBuilt = false;
	
	public EditPlaylist(MediaController controller) {
		user = NimbusUI.getCurrentUser();
		mediaService  = NimbusUI.getMediaLibraryService();
		this.controller = controller;
		
		setMargin(true);
		setSpacing(true);
	}
	
	public void showDialog() {
		List<Object> nmgs = controller.getGroupSelector().getSelectedItems();
		if (nmgs.isEmpty()) {
			Notification.show("Select a playlist to edit!");
			return;
		}
		if (!(nmgs.get(0) instanceof Playlist)) {
			Notification.show("That's not a playlist!");
			return;
		}
		
		this.selected = (Playlist) nmgs.get(0);
		if (!layoutBuilt) buildLayout();
		refresh();

		popup = new PopupWindow(caption, this);
		popup.setSubmitCaption("Save");
		popup.addSubmitListener(this);
		popup.open();
	}
	
	public String getCaption() {
		return caption;
	}
	
	private void refresh() {
		newName.setValue(selected.getName());
		playlist.removeAllItems();
		addPlaylistContents();
		groupSelect.setValue("Songs");
	}
	
	private void buildLayout() {
		// Edit Playlist name
		newName = new TextField("Playlist Name: ");
		addComponent(newName);
		
		// Group filters, source and target song tables
		createGroupSelect();
		createGroupFilter();
		createCandidates();
		createPlaylist();
		
		// Add Songs
		Button add = new Button("", new ClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				Collection<?> cs = (Collection<?>) candidates.getValue(); 
				List<Song> addList =  new ArrayList<Song>();
				// Single select is 0 length collection
				if (cs.size() > 0) {
					for (Object o: cs) {
						addList.add(((SongBean) o).getSong());
					}
				} else {
					if (candidates.getValue() instanceof Song)
						addList.add(((SongBean) playlist.getValue()).getSong());
				}
				if (addList.isEmpty()) return;
				for (Song add: addList) {
					SongBean ab = new SongBean(add);
					if (!pc.containsId(ab))
						pc.addBean(ab);
				}
			}
		});
		add.setIcon(FontAwesome.ANGLE_DOUBLE_RIGHT);
		add.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		// Remove Songs
		Button remove = new Button("", new ClickListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				Collection<?> cs = (Collection<?>) playlist.getValue(); 
				List<SongBean> removeList =  new ArrayList<SongBean>();
				// Single select is 0 length collection
				if (cs.size() > 0) {
					for (Object o: cs) {
						removeList.add((SongBean) o);
					}
				} else {
					if (playlist.getValue() instanceof SongBean)
						removeList.add((SongBean) playlist.getValue());
				}
				if (removeList.isEmpty()) return;
				
				for (SongBean remove: removeList) {
					playlist.removeItem(remove);
				}
			}
		});
		remove.setIcon(FontAwesome.ANGLE_DOUBLE_LEFT);
		remove.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		
		// Add components, show window
		HorizontalLayout groupLayout = new HorizontalLayout();
		groupLayout.setSpacing(true);
		groupLayout.setSizeUndefined();
		groupLayout.addComponent(groupSelect);
		groupLayout.addComponent(groupFilter);
		addComponent(groupLayout);
		
		// Add/Remove songs
		HorizontalLayout sLayout = new HorizontalLayout();
		sLayout.setSpacing(true);
		sLayout.setSizeUndefined();
		sLayout.addComponent(candidates);
		VerticalLayout sbLayout = new VerticalLayout();
		sbLayout.setSpacing(true);
		sbLayout.setSizeUndefined();
		sbLayout.addComponent(add);
		sbLayout.addComponent(remove);
		sbLayout.setComponentAlignment(add, Alignment.MIDDLE_CENTER);
		sbLayout.setComponentAlignment(remove, Alignment.MIDDLE_CENTER);
		remove.setSizeFull();
		sLayout.addComponent(sbLayout);
		sLayout.setComponentAlignment(sbLayout, Alignment.MIDDLE_CENTER);
		sLayout.addComponent(playlist);
		addComponent(sLayout);
		
		layoutBuilt = true;
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		
		Collection<?> currentItems = (Collection<?>) playlist.getItemIds(); 
		List<Song> newList =  new ArrayList<Song>();
		// Single select is 0 length collection
		if (currentItems.size() > 0) {
			for (Object o: currentItems) {
				newList.add(((SongBean) o).getSong());
			}
		} else {
			if (playlist.getItemIds() instanceof SongBean)
				newList.add(((SongBean) playlist.getValue()).getSong());
		}
		
		selected.setName(newName.getValue());
		if (!controller.save(selected, newList)) {
			Notification.show("There was an unexpected error", Notification.Type.ERROR_MESSAGE);
			return;
		}
		
		controller.setCurrentGroup(selected);
		controller.refresh();
		
		Notification.show("Playlist updated!");
		popup.close();
	}
	
	private void createGroupSelect() {
		// Select group type
		groupSelect = new ComboBox();
		groupSelect.setImmediate(true);
		groupSelect.setNullSelectionAllowed(false);
		groupSelect.addItem(MediaState.ARTISTS.display());
		groupSelect.addItem(MediaState.ALBUMS.display());
		groupSelect.addItem(MediaState.SONGS.display());
		groupSelect.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void valueChange(ValueChangeEvent event) {
				state = MediaState.valueOf(((String) groupSelect.getValue()).toUpperCase());
				refreshGroupFilter();
			}
		});
	}
	
	private void createGroupFilter() {
		groupFilter = new ComboBox();
		groupFilter.setNullSelectionAllowed(false);
		groupFilter.setImmediate(true);
		groupFilter.addValueChangeListener(new ValueChangeListener() {
			private static final long serialVersionUID = 1L;
			@Override
			public void valueChange(ValueChangeEvent event) {
				MediaGroup group = (MediaGroup) event.getProperty().getValue();
				if (group == null) return;
				changeGroupSelection(group);
			}
		});
		groupFilter.setEnabled(false);
	}
	
	private void refreshGroupFilter() {
		if (state == MediaState.ARTISTS) {
			groupFilter.setEnabled(true);
			groupFilter.removeAllItems();
			for (Artist artist: NimbusUI.getMediaLibraryService().getArtists(user)) {
				groupFilter.addItem(artist);
				groupFilter.setItemCaption(artist, artist.getName());
			}
		} else if (state == MediaState.ALBUMS) {
			groupFilter.setEnabled(true);
			groupFilter.removeAllItems();
			for (Album album: NimbusUI.getMediaLibraryService().getAlbums(user)) {
				groupFilter.addItem(album);
				groupFilter.setItemCaption(album, album.getName());
			}
		} else { // SONGS
			groupFilter.setEnabled(false);
			groupFilter.removeAllItems();
			showSongs();
		}
	}
		
	private void changeGroupSelection(MediaGroup group) {
		currentGroup = group;
		candidates.refresh();
		overrideCandidatesTableColumnConfig();
	}
	
	private void showSongs() {
		state = MediaState.SONGS;
		currentGroup = null;
		candidates.refresh();
		overrideCandidatesTableColumnConfig();
	}
	
	private void overrideCandidatesTableColumnConfig() {
		candidates.setVisibleColumns(new Object[] {
				SongBean.PROPERTY_TITLE, SongBean.PROPERTY_ARTIST
			});
		candidates.setColumnHeaders(new String [] { "Title", "Artist" });
		candidates.setColumnAlignments(Align.LEFT, Align.LEFT);
		candidates.setPageLength(10);
	}
	
	private void createCandidates() {
		candidates = new SongTable(this);
		candidates.addStyleName(ValoTheme.TABLE_SMALL);
		candidates.setWidth("300px");
		candidates.setPageLength(10);
		candidates.setSelectable(true);
		candidates.setMultiSelect(true);
		
		showSongs();
	}
	
	private void createPlaylist() {
		playlist = new Table();
		playlist.addStyleName(ValoTheme.TABLE_SMALL);
		playlist.setWidth("300px");
		playlist.setPageLength(10);
		playlist.setSelectable(true);
		playlist.setMultiSelect(true);
		pc = new BeanItemContainer<SongBean>(SongBean.class);
		playlist.setContainerDataSource(pc);
		playlist.setVisibleColumns(new Object[] {
			SongBean.PROPERTY_TITLE, SongBean.PROPERTY_ARTIST
		});
			
		playlist.setColumnHeaders(new String [] { "Title", "Artist" });
		playlist.setColumnAlignments(Align.LEFT, Align.LEFT);
	}
	
	private void addPlaylistContents() {
		// Playlist was lazily added (without contents) to the group select
		for (Song s : NimbusUI.getMediaLibraryService().getPlaylistSongs(selected)) {
			pc.addBean(new SongBean(s));
		}
	}

	@Override
	public List<Song> getCurrentSongs(SongBeanQuery instance, int startIndex, int count) {
		if (state == MediaState.SONGS) return mediaService.getSongs(user, startIndex, count);
		else if (currentGroup instanceof Artist) return mediaService.getArtistSongs(user, (Artist) currentGroup, startIndex, count);
		else if (currentGroup instanceof Album) return mediaService.getAlbumSongs(user, (Album) currentGroup, startIndex, count);
		else if (currentGroup instanceof Playlist) return mediaService.getPlaylistSongs((Playlist) currentGroup, startIndex, count);
		return Collections.emptyList();
	}
	
	@Override
	public int getCurrentSongCount(SongBeanQuery instance) {
		if (state == MediaState.SONGS) return mediaService.getSongCount(user);
		else if (currentGroup instanceof Artist) return mediaService.getArtistSongCount(user, (Artist) currentGroup);
		else if (currentGroup instanceof Album) return mediaService.getAlbumSongCount(user, (Album) currentGroup);
		else if (currentGroup instanceof Playlist) return mediaService.getPlaylistSongCount((Playlist) currentGroup);
		return 0;
	}
	
	@Override
	public MediaState getState(SongTable instance) {
		return state;
	}

	@Override
	public void handleClick(SongTable instance, SongBean clicked, boolean isDoubleClick) {
		// Do nothing
	}
}