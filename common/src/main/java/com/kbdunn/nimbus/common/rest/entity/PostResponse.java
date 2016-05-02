package com.kbdunn.nimbus.common.rest.entity;

import java.util.Date;

public class PostResponse {
	private Long id;
	private Date created;
	private Date updated;
	private String[] uris;
	
	public PostResponse() { }
	
	public PostResponse(Long id, Date created, Date updated, String... uris) {
		this.id = id;
		this.created = created;
		this.updated = updated;
		this.uris = uris;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreated() {
		return created;
	}

	public void setCreated(Date created) {
		this.created = created;
	}

	public Date getUpdated() {
		return updated;
	}

	public void setUpdated(Date updated) {
		this.updated = updated;
	}

	public String[] getUris() {
		return uris;
	}

	public void setUris(String[] uris) {
		this.uris = uris;
	}
}
