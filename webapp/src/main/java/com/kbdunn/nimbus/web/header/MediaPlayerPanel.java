package com.kbdunn.nimbus.web.header;

import com.kbdunn.vaadin.addons.mediaelement.MediaElementPlayer;
import com.kbdunn.vaadin.addons.mediaelement.MediaElementPlayerOptions;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.web.theme.NimbusTheme;
import com.kbdunn.vaadin.addons.fontawesome.FontAwesome;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class MediaPlayerPanel extends Panel implements ClickListener {

	private static final long serialVersionUID = 1L;
	
	private Button toggleButton;
	private MediaPlayer mediaPlayer;
	private HorizontalLayout content;
	private VerticalLayout playerContent;
	private HorizontalLayout controlLayout;
	private Label nowPlaying;
	private MediaElementPlayer audioPlayer;
	private boolean isToggled = false;

	public MediaPlayerPanel(final MediaPlayer mediaPlayer) {
		addStyleName("media-player-panel");
		addStyleName(ValoTheme.PANEL_WELL);
		setSizeUndefined();
		setHeight("70px");
		this.mediaPlayer = mediaPlayer;
		
		initializeToggleButton();
		initializeAudioPlayer();
		buildLayout();
	}
	
	public MediaElementPlayer getMejsPlayer() {
		return audioPlayer;
	}
	
	public MediaPlayer getMediaController() {
		return mediaPlayer;
	}
	
	public Button getToggleButton() {
		return toggleButton;
	}
	
	private void buildLayout() {
		content = new HorizontalLayout();
		content.setWidth("100%");
		content.addComponent(toggleButton);
		setContent(content);
		
		playerContent = new VerticalLayout();
		playerContent.setWidth("100%");
		playerContent.addStyleName("player");
		content.addComponent(playerContent);
		
		nowPlaying = new Label();
		nowPlaying.setContentMode(ContentMode.HTML);
		nowPlaying.setHeight("25px");
		nowPlaying.addStyleName("now-playing");
		nowPlaying.addStyleName(ValoTheme.LABEL_BOLD);
		nowPlaying.addStyleName(ValoTheme.LABEL_SMALL);
		playerContent.addComponent(nowPlaying);
		playerContent.setComponentAlignment(nowPlaying, Alignment.MIDDLE_CENTER);
		
		// Last, Play/Pause, Next
		controlLayout = new HorizontalLayout();
		buildControlLayout();
		playerContent.addComponent(controlLayout);
		playerContent.setComponentAlignment(controlLayout, Alignment.MIDDLE_CENTER);
	}
	
	private void buildControlLayout() {
		Button prev = new Button(FontAwesome.FAST_BACKWARD);
		prev.addClickListener(new ClickListener() {

			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				mediaPlayer.playPrevious();
			}
		});
		prev.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		prev.addStyleName("media-player-button");
		
		Button next = new Button(FontAwesome.FAST_FORWARD);
		next.addClickListener(new ClickListener() {

			private static final long serialVersionUID = 1L;
			@Override
			public void buttonClick(ClickEvent event) {
				mediaPlayer.playNext();
			}
		});
		next.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		next.addStyleName("media-player-button");
		
		final Button repeat = new Button("1");
		if (mediaPlayer.isRepeatAll()) {
			repeat.addStyleName("media-player-repeat-enabled");
			if (mediaPlayer.isRepeatOne()) {
				repeat.addStyleName("media-player-repeat-one-enabled");
			} else {
				repeat.removeStyleName("media-player-repeat-one-enabled");
			}
		} else {
			repeat.removeStyleName("media-player-repeat-enabled");
			repeat.removeStyleName("media-player-repeat-one-enabled");
		}
		repeat.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		repeat.setIcon(FontAwesome.REPEAT);
		repeat.addStyleName("media-player-repeat");
		repeat.addStyleName("media-player-button");
		repeat.addClickListener(new ClickListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				boolean r = mediaPlayer.isRepeatAll();
				boolean o = mediaPlayer.isRepeatOne();
				if (r && o) {
					mediaPlayer.setRepeatOne(false);
					mediaPlayer.setRepeatAll(false);
					repeat.removeStyleName("media-player-repeat-enabled");
					repeat.removeStyleName("media-player-repeat-one-enabled");
				} else if (r) {
					mediaPlayer.setRepeatOne(true);
					repeat.addStyleName("media-player-repeat-one-enabled");
				} else {
					mediaPlayer.setRepeatAll(true);
					repeat.addStyleName("media-player-repeat-enabled");
				}
			}
		});
		final Button shuffle = new Button();
		shuffle.addStyleName("media-player-shuffle");
		if (mediaPlayer.isShuffle()) {
			repeat.addStyleName("media-player-shuffle-enabled");
		} else {
			repeat.removeStyleName("media-player-shuffle-enabled");
		}
		shuffle.addStyleName("media-player-button");
		shuffle.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		shuffle.setIcon(FontAwesome.RANDOM);
		shuffle.addClickListener(new ClickListener() {

			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event) {
				boolean s = mediaPlayer.isShuffle() ? false : true;
				mediaPlayer.setShuffle(s);
				if (s) {
					shuffle.addStyleName("media-player-shuffle-enabled");
				} else {
					shuffle.removeStyleName("media-player-shuffle-enabled");
				}
			}
		});
		
		controlLayout.addComponent(prev);
		controlLayout.addComponent(repeat);
		controlLayout.addComponent(audioPlayer);
		controlLayout.addComponent(shuffle);
		controlLayout.addComponent(next);
	}
	
	private void initializeAudioPlayer() {
		MediaElementPlayerOptions opts = new MediaElementPlayerOptions();
		opts.setFeatures(new MediaElementPlayerOptions.Feature[] { 
				MediaElementPlayerOptions.Feature.PLAYPAUSE, 
				MediaElementPlayerOptions.Feature.PROGRESS, 
				MediaElementPlayerOptions.Feature.CURRENT, 
				MediaElementPlayerOptions.Feature.DURATION });
		opts.setAudioHeight(24);
		opts.setAudioWidth(175);
		audioPlayer = new MediaElementPlayer(MediaElementPlayer.Type.AUDIO, opts);
		audioPlayer.addStyleName(NimbusTheme.MEDIA_ELEMENT_PLAYER);
		audioPlayer.addPlaybackEndedListener(mediaPlayer);
	}
	
	public void setNowPlayingInfo() {
		Song playing = (Song) mediaPlayer.getPlaying();
		nowPlaying.setValue("<marquee behavior=\"scroll\" direction=\"left\">"
				+ playing.getTitle() + " by "
				+ playing.getArtist() + " on "
				+ playing.getAlbum()
				+ "</marquee>");
	}
	
	private void initializeToggleButton() {
		toggleButton = new Button();
		toggleButton.setIcon(FontAwesome.VOLUME_UP);
		toggleButton.addStyleName(ValoTheme.BUTTON_BORDERLESS);
		toggleButton.addStyleName("player-toggle-button");
		toggleButton.addClickListener(this);
	}

	@Override
	public void buttonClick(ClickEvent event) {
		isToggled = !isToggled;
		if (isToggled) {
			addStyleName("media-player-panel-toggled");
		} else {
			removeStyleName("media-player-panel-toggled");
		}
	}
}