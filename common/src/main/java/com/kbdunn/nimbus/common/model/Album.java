package com.kbdunn.nimbus.common.model;

import com.kbdunn.nimbus.common.util.ComparatorUtil;

public class Album implements MediaGroup, Comparable<Album> {
	
	public static final String UNKNOWN = "Unknown";
	
	private String name;
	private String artistName;
	
	public Album(String name, String artistName) {
		this.name = name;
		this.artistName = artistName;
	}
	
	public String getName() {
		return name == null || name.isEmpty() ? UNKNOWN : name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getArtistName() {
		return artistName == null || artistName.isEmpty() ? UNKNOWN : artistName;
	}
	
	public void setArtistName(String artistName) {
		this.artistName = artistName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getArtistName() == null) ? 0 : getArtistName().hashCode());
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Album))
			return false;
		Album other = (Album) obj;
		if (getArtistName() == null) {
			if (other.getArtistName() != null)
				return false;
		} else if (!getArtistName().equals(other.getArtistName()))
			return false;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Album [name=" + getName() + ", artistName=" + getArtistName() + "]";
	}

	@Override
	public int compareTo(Album o) {
		int i = ComparatorUtil.nullSafeStringComparator(this.artistName, o.artistName);
		i = i == 0 ? ComparatorUtil.nullSafeStringComparator(this.name, o.name) : i;
		return i;
	}
}
