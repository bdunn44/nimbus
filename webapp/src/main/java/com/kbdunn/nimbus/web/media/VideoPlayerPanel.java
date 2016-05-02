package com.kbdunn.nimbus.web.media;

import java.io.File;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.mediaelement.MediaElementPlayer;
import com.kbdunn.vaadin.addons.mediaelement.MediaElementPlayerOptions;
import com.vaadin.server.FileResource;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;

public class VideoPlayerPanel extends Panel {

	private static final long serialVersionUID = 1L;
	
	private MediaElementPlayer videoPlayer;

	public VideoPlayerPanel() {
		setSizeUndefined();
		addStyleName("video-player-panel");
		addStyleName(ValoTheme.PANEL_WELL);
		
		MediaElementPlayerOptions opts = new MediaElementPlayerOptions();
		opts.setVideoHeight(250);
		opts.setVideoWidth(350);
		opts.setEnableAutosize(false);
		opts.setPauseOtherPlayers(true);
		
		videoPlayer = new MediaElementPlayer(MediaElementPlayer.Type.VIDEO, opts);
		videoPlayer.addStyleName(NimbusTheme.MEDIA_ELEMENT_PLAYER);
		
		setContent(videoPlayer);
	}
	
	public void play(NimbusFile video) {
		videoPlayer.setSource(new FileResource(new File(video.getPath())));
		videoPlayer.play();
	}
}