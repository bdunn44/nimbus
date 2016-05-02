package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class ShareBlockAccess implements NimbusRecord {

	private Long id;
	private Long shareBlockId;
	private Long userId;
	private Boolean canCreate;
	private Boolean canUpdate;
	private Boolean canDelete;
	private Date created;
	private Date updated;
	
	public ShareBlockAccess(Long shareBlockId, Long userId) {
		this.shareBlockId = shareBlockId;
		this.userId = userId;
		this.canCreate = false;
		this.canUpdate = false;
		this.canDelete = false;
	}
	
	public ShareBlockAccess(Long id, Long shareBlockId, Long userId,
			Boolean canCreate, Boolean canUpdate, Boolean canDelete,
			Date createDate, Date lastUpdateDate) {
		super();
		this.id = id;
		this.shareBlockId = shareBlockId;
		this.userId = userId;
		this.canCreate = canCreate;
		this.canUpdate = canUpdate;
		this.canDelete = canDelete;
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
	
	public Long getUserId() {
		return userId;
	}
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public boolean canCreate() {
		return canCreate != null && canCreate;
	}
	
	public void setCanCreate(Boolean canCreate) {
		this.canCreate = canCreate;
	}
	
	public boolean canUpdate() {
		return canUpdate != null && canUpdate;
	}
	
	public void setCanUpdate(Boolean canUpdate) {
		this.canUpdate = canUpdate;
	}
	
	public boolean canDelete() {
		return canDelete != null && canDelete;
	}
	
	public void setCanDelete(Boolean canDelete) {
		this.canDelete = canDelete;
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
		result = prime * result
				+ ((canCreate == null) ? 0 : canCreate.hashCode());
		result = prime * result
				+ ((canDelete == null) ? 0 : canDelete.hashCode());
		result = prime * result
				+ ((canUpdate == null) ? 0 : canUpdate.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((shareBlockId == null) ? 0 : shareBlockId.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ShareBlockAccess))
			return false;
		ShareBlockAccess other = (ShareBlockAccess) obj;
		if (canCreate == null) {
			if (other.canCreate != null)
				return false;
		} else if (!canCreate.equals(other.canCreate))
			return false;
		if (canDelete == null) {
			if (other.canDelete != null)
				return false;
		} else if (!canDelete.equals(other.canDelete))
			return false;
		if (canUpdate == null) {
			if (other.canUpdate != null)
				return false;
		} else if (!canUpdate.equals(other.canUpdate))
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
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ShareBlockAccess [id=" + id + ", shareBlockId=" + shareBlockId
				+ ", userId=" + userId + ", canCreate=" + canCreate
				+ ", canUpdate=" + canUpdate + ", canDelete=" + canDelete + "]";
	}
}
