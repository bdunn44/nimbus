package com.kbdunn.nimbus.web.bean;

import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.common.util.StringUtil;

public class VideoBean {
	
	public static final String PROPERTY_ITEM_ID = "itemId";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_LENGTH = "lengthString";

	private Video video;
	
	public VideoBean() {  }
	
	public VideoBean(Video video) {
		this.video = video;
	}
	
	public Video getVideo() {
		return video;
	}
	
	public Object getItemId() {
		return this;
	}
	
	public Long getId() {
		return video.getId();
	}
	
	public String getTitle() {
		return video.getTitle();
	}
	
	public String getLengthString() {
		return StringUtil.toDurationString(video.getLength());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((video == null) ? 0 : video.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof VideoBean))
			return false;
		VideoBean other = (VideoBean) obj;
		if (video == null) {
			if (other.video != null)
				return false;
		} else if (!video.equals(other.video))
			return false;
		return true;
	}
}