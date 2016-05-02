package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.kbdunn.nimbus.common.util.ComparatorUtil;

public class Playlist implements NimbusRecord, MediaGroup, Comparable<Playlist> {

	private Long id;
	private Long userId;
	private String name;
	private Date created;
	private Date updated;
	
	public Playlist(Long userId, String name) {
		this.userId = userId;
		this.name = name;
	}
	
	public Playlist(Long id, Long userId, String name, Date createDate, Date lastUpdateDate) {
		this.id = id;
		this.userId = userId;
		this.name = name;
		this.created = createDate;
		this.updated = lastUpdateDate;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public void setId(Long id) {
		this.id = id;
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public void setUserId(Long nimbusUserId) {
		this.userId = nimbusUserId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Date getCreated() {
		return created;
	}

	@Override
	public void setCreated(Date createDate) {
		this.created = createDate;
	}

	@Override
	public Date getUpdated() {
		return updated;
	}

	@Override
	public void setUpdated(Date lastUpdateDate) {
		this.updated = lastUpdateDate;
	}
	
	@Override
	public String toString() {
		return "Playlist [id=" + id + ", userId=" + userId + ", name=" + name
				+ "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Playlist))
			return false;
		Playlist other = (Playlist) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public int compareTo(Playlist o) {
		int i = ComparatorUtil.nullSafeLongComparator(this.userId, o.userId);
		i = i == 0 ? ComparatorUtil.nullSafeStringComparator(this.name, o.name) : i;
		return i;
	}
}
