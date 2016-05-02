package com.kbdunn.nimbus.web.media.action;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

public class CreatePlaylist extends HorizontalLayout implements ClickListener {

	private static final long serialVersionUID = 6614853590075931749L;
	
	private NimbusUser user;
	private MediaController mediaBrowser;
	private PopupWindow popup;
	private TextField playlistName;
	
	private static final String caption = "Create Playlist";
	
	public CreatePlaylist(MediaController mediaBrowser) {
		user = (NimbusUser) UI.getCurrent().getSession().getAttribute("user");
		this.mediaBrowser = mediaBrowser;
		
		buildLayout();
	}
	
	public void showDialog() {
		playlistName.setValue("");
		popup = new PopupWindow(caption, this);
		popup.setSubmitCaption("Create");
		popup.addSubmitListener(this);
		popup.open();
	}
	
	public String getCaption() {
		return caption;
	}
	
	private void buildLayout() {
		// Input field for folder name
		playlistName = new TextField("Playlist Name");
		addComponent(playlistName);
	}
	
	@Override
	public void buttonClick(ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		
		if (playlistName.getValue().isEmpty()) {
			playlistName.setComponentError(new UserError("Enter a name for the playlist"));
			return;
		}
		
		if (NimbusUI.getMediaLibraryService().getPlaylistByName(user, playlistName.getValue()) != null) {
			playlistName.setComponentError(new UserError("A playlist with that name already exists"));
			return;
		}
		
		Playlist newPlaylist = new Playlist(user.getId(), playlistName.getValue());
		NimbusUI.getMediaLibraryService().save(newPlaylist);
		
		mediaBrowser.setCurrentGroup(newPlaylist);
		mediaBrowser.getGroupSelector().refresh();
		
		Notification.show("Playlist created!");
		popup.close();
	}
}