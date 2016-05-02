package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class ShareBlockRecipient implements NimbusRecord {

	private Long id;
	private Long shareBlockId;
	private String email;
	private Date created;
	private Date updated;
	
	public ShareBlockRecipient(Long shareBlockId, String email) {
		this.shareBlockId = shareBlockId;
		this.email = email;
	}
	
	public ShareBlockRecipient(Long id, Long shareBlockId, String email,
			Date createDate, Date lastUpdateDate) {
		super();
		this.id = id;
		this.shareBlockId = shareBlockId;
		this.email = email;
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
	
	public Long getShareBlockId() {
		return shareBlockId;
	}
	
	public void setShareBlockId(Long shareBlockId) {
		this.shareBlockId = shareBlockId;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((shareBlockId == null) ? 0 : shareBlockId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ShareBlockRecipient))
			return false;
		ShareBlockRecipient other = (ShareBlockRecipient) obj;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (shareBlockId == null) {
			if (other.shareBlockId != null)
				return false;
		} else if (!shareBlockId.equals(other.shareBlockId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ShareBlockRecipient [id=" + id + ", shareBlockId="
				+ shareBlockId + ", email=" + email + "]";
	}
}
