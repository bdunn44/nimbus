package com.kbdunn.nimbus.web.media.action;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.media.MediaState;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

public class EditMetadata extends VerticalLayout implements ClickListener {
	
	private static final long serialVersionUID = 354112259530474172L;
	
	private MediaController controller;
	private PopupWindow popup;
	private Label info;
	private TextField title, artist, album, trackNo, year;
	private List<Song> selected;
	private boolean layoutBuilt = false;
	
	public EditMetadata(MediaController controller) {
		this.controller = controller;
	}
	
	private void buildLayout() {
		setSpacing(true);
		
		info = new Label();
		title = new TextField("Title");
		artist = new TextField("Artist");
		album = new TextField("Album");
		trackNo = new TextField("Track #");
		year = new TextField("Year");

		info.setSizeUndefined();
		info.addStyleName(NimbusTheme.LABEL_CAPTION_ONLY);
		title.setWidth("400px");
		artist.setWidth("400px");
		album.setWidth("400px");
		trackNo.setWidth("100px");
		year.setWidth("100px");
		
		addComponent(info);
		addComponent(title);
		addComponent(artist);
		addComponent(album);
		addComponent(year);
		addComponent(trackNo);
		
		layoutBuilt = true;
	}
	
	private boolean refresh() {
		if (controller.getState() == MediaState.VIDEOS) return false;
		List<NimbusFile> items = controller.getSelectedMediaFiles();
		if (items.isEmpty()) {
			Notification.show("Select one or more songs to edit");
			return false;
		}
		info.setCaption("Editing " + items.size() + " song" + (items.size() > 1 ? "s" : ""));
		if (items.size() > 1) {
			info.setIcon(FontAwesome.EXCLAMATION_TRIANGLE);
		} else {
			info.setIcon(null);
		}
		
		selected = new ArrayList<Song>();
		Song s = null;
		String art = null;
		String alb = null;
		Integer tr = null; 
		String y = null;
		for (NimbusFile nf : items) {
			s = (Song) nf;
			selected.add(s);
			art = art == null ? s.getArtist() : !art.equals(s.getArtist()) ? "" : art;
			alb = alb == null ? s.getAlbum() : !alb.equals(s.getAlbum()) ? "" : alb;
			tr = tr == null ? s.getTrackNumber() : !tr.equals(s.getTrackNumber()) ? null : tr;
			y = y == null ? s.getAlbumYear() : !y.equals(s.getAlbumYear()) ? "" : y;
		}
		if (selected.size() == 1) {
			title.setValue(selected.get(0).getTitle());
			title.setVisible(true);
			if (tr != null) {
				trackNo.setValue(tr.toString());
			} else {
				trackNo.setValue("");
			}
			trackNo.setVisible(true);
		} else {
			title.setVisible(false);
			trackNo.setVisible(false);
		}
		
		if (art != null) artist.setValue(art);
		if (album != null) album.setValue(alb);
		if (year != null) year.setValue(y);
		
		return true;
	}
	
	public void showDialog() {
		if (!layoutBuilt) buildLayout();
		if (!refresh()) return;
		popup = new PopupWindow("Edit Song Information", this);
		popup.setSubmitCaption("Save");
		popup.addSubmitListener(this);
		popup.open();
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		Integer newTrackNo = null;
		try {
			if (!trackNo.getValue().isEmpty()) 
				newTrackNo = Integer.valueOf(trackNo.getValue());
		} catch (NumberFormatException e) {
			trackNo.setComponentError(new UserError("Please enter a valid number"));
			return;
		}
		String newYear = year.getValue();
		List<Song> songs = new ArrayList<Song>();
		for (Song s : selected) {
			if (title.isVisible()) s.setTitle(title.getValue());
			s.setArtist(artist.getValue());
			s.setAlbum(album.getValue());
			s.setTrackNumber(newTrackNo);
			s.setAlbumYear(newYear);
			songs.add(s);
		}
		
		if (!controller.save(songs)) {
			Notification.show("There was an unexpected error", Notification.Type.ERROR_MESSAGE);
		}
		popup.close();
	}
}
