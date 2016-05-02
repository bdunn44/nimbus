package com.kbdunn.nimbus.common.model;

import com.kbdunn.nimbus.common.util.ComparatorUtil;

public class Artist implements MediaGroup, Comparable<Artist> {

	public static final String UNKNOWN = "Unknown";
	
	private String name;

	public Artist(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name == null || name.isEmpty() ? UNKNOWN : name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getName() == null) ? 0 : getName().hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Artist))
			return false;
		Artist other = (Artist) obj;
		if (getName() == null) {
			if (other.getName() != null)
				return false;
		} else if (!getName().equals(other.getName()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Artist [name=" + getName() + "]";
	}

	@Override
	public int compareTo(Artist o) {
		return ComparatorUtil.nullSafeStringComparator(this.name, o.name);
	}
}