package com.kbdunn.nimbus.web.bean;

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

public class MediaGroupBean {
	
	public static final String PROPERTY_ITEM_ID = "itemId";
	public static final String PROPERTY_NAME = "name";
	private MediaGroup group;

	public MediaGroupBean() {  }
	
	public MediaGroupBean(MediaGroup group) {
		this.group = group;
	}
	
	public MediaGroup getMediaGroup() {
		return group;
	}
	
	public Object getItemId() {
		return this;
		/*if (group instanceof Playlist) return ((Playlist) group).getId();
		else if (group instanceof Artist) return getName();
		else return ((Album) group).getArtistName() + "|" + getName();*/
	}
	
	public String getName() {
		return group == null ? null : group.getName();
	}
	
	public List<Song> getSongs() {
		MediaLibraryService mService = NimbusUI.getMediaLibraryService();
		NimbusUser user = NimbusUI.getCurrentUser();
		if (group instanceof Artist) {
			return mService.getArtistSongs(user, (Artist) group);
		}
		if (group instanceof Album) {
			return mService.getAlbumSongs(user, (Album) group);
		}
		if (group instanceof Playlist) {
			return mService.getPlaylistSongs((Playlist) group);
		}
		return Collections.emptyList();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MediaGroupBean))
			return false;
		MediaGroupBean other = (MediaGroupBean) obj;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		return true;
	}
}