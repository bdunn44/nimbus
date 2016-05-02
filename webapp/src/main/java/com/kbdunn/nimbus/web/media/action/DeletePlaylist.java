package com.kbdunn.nimbus.web.media.action;

import java.util.List;

import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.popup.PopupWindow;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;

public class DeletePlaylist extends VerticalLayout implements ClickListener {

	private static final long serialVersionUID = 3717519621263553437L;
	
	private MediaController controller;
	private PopupWindow popup;
	private Label desc;
	private List<?> selected;
	private boolean layoutBuilt = false;
	
	public DeletePlaylist(MediaController controller) {
		this.controller = controller;
	}
	
	public void showDialog() {
		List<?> nmgs = controller.getGroupSelector().getSelectedItems();
		if (nmgs.isEmpty()) {
			Notification.show("Select a playlist to delete!");
			return;
		}
		
		selected = nmgs;
		if (!layoutBuilt) buildLayout();
		refresh();

		popup = new PopupWindow("Delete Playlist", this);
		popup.setSubmitCaption("Delete");
		popup.addSubmitListener(this);
		popup.open();
	}
	
	private void buildLayout() {
		setMargin(true);
		setSpacing(true);
		
		desc = new Label();
	}
	
	private void refresh() {
		removeAllComponents();
		
		String caption = "Delete ";
		caption += selected.size() > 1 ? "these playlists?" : "this playlist?";
		desc.setValue(caption);
		addComponent(desc);
		
		for (Object group: selected) {
			String name = null;
			//if (group instanceof Artist) name = ((Artist) group).getName();
			//if (group instanceof Album) name = ((Album) group).getName();
			if (group instanceof Playlist) name = ((Playlist) group).getName();
			if (name == null)
				throw new IllegalStateException("Selected object is not an Artist, Album or Playlist object");
			addComponent(new Label(name));
		}
	}
	
	@Override
	public void buttonClick(Button.ClickEvent event) {
		if (NimbusUI.getPropertiesService().isDemoMode()) return;
		
		Object current = controller.getCurrentGroup();
		for (Object group: selected) {
			if (group.equals(current)) controller.setCurrentGroup(null);
			NimbusUI.getMediaLibraryService().delete((Playlist) group);
		}

		controller.refresh();
		
		String n = "Playlist";
		n += selected.size() > 1 ? "s deleted!" : " deleted!";
		Notification.show(n);
		popup.close();
	}
}