package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class ShareBlockFile implements NimbusRecord {
	
	private Long id;
	private Long nimbusFileId;
	private Long shareBlockId;
	private String note;
	private Date created;
	private Date updated;
	
	public ShareBlockFile(Long nimbusFileId, Long shareBlockId, String note) {
		this.nimbusFileId = nimbusFileId;
		this.shareBlockId = shareBlockId;
		this.note = note;
	}
	
	public ShareBlockFile(Long id, Long nimbusFileId, Long shareBlockId,
			String note, Date createDate, Date lastUpdateDate) {
		super();
		this.id = id;
		this.nimbusFileId = nimbusFileId;
		this.shareBlockId = shareBlockId;
		this.note = note;
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
	
	public Long getNimbusFileId() {
		return nimbusFileId;
	}
	
	public void setNimbusFileId(Long nimbusFileId) {
		this.nimbusFileId = nimbusFileId;
	}
	
	public Long getShareBlockId() {
		return shareBlockId;
	}
	
	public void setShareBlockId(Long shareBlockId) {
		this.shareBlockId = shareBlockId;
	}
	
	public String getNote() {
		return note;
	}
	
	public void setNote(String note) {
		this.note = note;
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((nimbusFileId == null) ? 0 : nimbusFileId.hashCode());
		result = prime * result + ((note == null) ? 0 : note.hashCode());
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
		if (!(obj instanceof ShareBlockFile))
			return false;
		ShareBlockFile other = (ShareBlockFile) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (nimbusFileId == null) {
			if (other.nimbusFileId != null)
				return false;
		} else if (!nimbusFileId.equals(other.nimbusFileId))
			return false;
		if (note == null) {
			if (other.note != null)
				return false;
		} else if (!note.equals(other.note))
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
		return "ShareBlockFile [id=" + id + ", nimbusFileId=" + nimbusFileId
				+ ", shareBlockId=" + shareBlockId + ", note=" + note + "]";
	}
}
