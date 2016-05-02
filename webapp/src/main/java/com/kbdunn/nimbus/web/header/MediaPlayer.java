package com.kbdunn.nimbus.web.header;

import java.io.File;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.web.media.MediaController;
import com.kbdunn.nimbus.web.media.MediaState;
import com.kbdunn.vaadin.addons.mediaelement.MediaElementPlayer;
import com.kbdunn.vaadin.addons.mediaelement.interfaces.PlaybackEndedListener;
import com.vaadin.server.FileResource;

public class MediaPlayer implements PlaybackEndedListener, Serializable {

	private static final long serialVersionUID = -856775619405971605L;
	private static final Logger log = LogManager.getLogger(MediaPlayer.class.getName());
	
	private boolean repeatOne;
	private boolean repeatAll;
	private boolean shuffle;
	private MediaPlayerPanel playerPanel;
	private MediaController mediaBrowser;
	
	private List<NimbusFile> currentPlayList;
	private NimbusFile playing;
	private Integer firstPlayedIndex;
	
	public MediaPlayer() { 	}
	
	public void setPlayerPanel(MediaPlayerPanel playerPanel) {
		this.playerPanel = playerPanel;
	}
	
	public void setMediaBrowser(MediaController mediaBrowser) {
		this.mediaBrowser = mediaBrowser;
	}
	
	public boolean isRepeatOne() {
		return repeatOne;
	}
	
	public void setRepeatOne(boolean repeat) {
		this.repeatOne = repeat;
	}
	
	public boolean isRepeatAll() {
		return repeatAll;
	}
	
	public void setRepeatAll(boolean repeatAll) {
		this.repeatAll = repeatAll;
	}
	
	public boolean isShuffle() {
		return shuffle;
	}
	
	public void setShuffle(boolean shuffle) {
		if (this.shuffle != shuffle) {
			this.shuffle = shuffle;
			refreshCurrentPlaylist();
			if (playing != null) firstPlayedIndex = currentPlayList.indexOf(playing);
		}
	}
	
	public NimbusFile getPlaying() {
		return playing;
	}

	public void play(NimbusFile item) {
		if (item == null) return;
		
		playing = item;
		//playerPanel.getMejsPlayer().stop();
		playerPanel.getMejsPlayer().setSource(new FileResource(new File(item.getPath())));
		playerPanel.getMejsPlayer().play();
		mediaBrowser.getItemSelector().select(item);
		if (mediaBrowser.getState() != MediaState.VIDEOS) {
			if (firstPlayedIndex == null) firstPlayedIndex = currentPlayList.indexOf(item);
			playerPanel.setNowPlayingInfo();
		}
	}
	
	public void stop() {
		playing = null;
		playerPanel.getMejsPlayer().pause();
		firstPlayedIndex = null;
	}
	
	public void playFirst(NimbusFile item) {
		refreshCurrentPlaylist();
		if (currentPlayList == null || currentPlayList.size() == 0) return;
		
		play(item);
		firstPlayedIndex = currentPlayList.indexOf(item);

		log.debug("PlayFirst called. firstPlayedIndex is " + firstPlayedIndex);
	}

	public void playNext() {
		if (currentPlayList == null || currentPlayList.size() == 0) {
			log.debug("Cannot play next song, current playlist is null or empty");
			return;
		}
		
		int nextIndex = currentPlayList.indexOf(playing) + 1;
		log.debug("PlayNext called. Current index: " + (nextIndex -1) + 
				". PL size: " + currentPlayList.size() + 
				". First played index: " + firstPlayedIndex);
		
		if (repeatOne) {
			log.info("Repeating...");
			play(playing);
			return;
		} else if (!repeatAll && firstPlayedIndex != null && nextIndex == firstPlayedIndex) {
			log.debug("Hit end of play list. Stopping.");
			stop();
			return;
		}

		mediaBrowser.unselectAll();
		if (nextIndex >= currentPlayList.size()) {
			log.debug("Hit end of play list, going to start");
			 if (repeatAll) {
				 play(currentPlayList.get(0));
			 } else {
				 stop();
			 }
		} else {
			log.debug("Playing next index");
			play(currentPlayList.get(nextIndex));
		}
	}

	public void playPrevious() {
		if (currentPlayList == null || currentPlayList.size() == 0) return;
		
		int prevIndex = currentPlayList.indexOf(playing) - 1;
		log.debug("PlayPrevious called. Current index: " + (prevIndex + 1) + 
				". PL size: " + currentPlayList.size() + 
				". First played index: " + firstPlayedIndex);
		
		if (repeatOne) {
			play(playing);
			return;
		} else if (!repeatAll && firstPlayedIndex != null && prevIndex < firstPlayedIndex) {
			stop();
			return;
		}

		mediaBrowser.unselectAll();
		if (currentPlayList.indexOf(playing) - 1 < 0) {
			if (repeatAll) {
				play(currentPlayList.get(currentPlayList.size() - 1));
			} else {
				stop();
			}
		} else {
			play(currentPlayList.get(currentPlayList.indexOf(playing) - 1));
		}
	}
	
	public void refreshCurrentPlaylist() { 
		if (mediaBrowser == null) return;
		
		currentPlayList =  mediaBrowser.getCurrentMediaFiles();
		if (shuffle) Collections.shuffle(currentPlayList);
	}

	@Override
	public void playbackEnded(MediaElementPlayer player) {
		playNext();
	}
}
