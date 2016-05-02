package com.kbdunn.nimbus.web.media;

import com.kbdunn.nimbus.web.NimbusUI;
import com.kbdunn.nimbus.web.controlbar.MediaControlBar;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.VerticalLayout;

public class MediaView extends VerticalLayout implements View {
	
	private static final long serialVersionUID = 1L;
	public static final String NAME = "media";
	
	private VideoPlayerPanel videoPanel;
	private MediaControlBar controlBar;
	private MediaBrowserPanel browserPanel;
	private MediaController controller;
	
	private boolean layoutBuilt = false;

	public MediaView() { }
	
	@Override
	public void enter(ViewChangeEvent event) {
		if (!layoutBuilt) {
			buildLayout();
			layoutBuilt = true;
		}
		controller.parseFragment(NAME + "/" + event.getParameters());
	}
	
	private void buildLayout() {
		controller = new MediaController();
		controlBar = new MediaControlBar(controller);
		browserPanel = new MediaBrowserPanel(controller);
		controller.setBrowserPanel(browserPanel);
		controller.setMediaControlBar(controlBar);
		NimbusUI.getCurrent().getMediaController().setMediaBrowser(controller);
		
		videoPanel = new VideoPlayerPanel();
		controller.setVideoPanel(videoPanel);

		addComponent(videoPanel);
		setComponentAlignment(videoPanel, Alignment.MIDDLE_CENTER);
		addComponent(controlBar);
		addComponent(browserPanel);
	}
}