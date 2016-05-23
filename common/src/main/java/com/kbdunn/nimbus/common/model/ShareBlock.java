package com.kbdunn.nimbus.common.model;

import java.util.Date;

import com.kbdunn.nimbus.common.util.ComparatorUtil;

public class ShareBlock implements NimbusRecord, FileContainer, Comparable<ShareBlock> {
	
	private Long id;
	private Long userId;
	private Boolean external;
	private Boolean externalUploadAllowed;
	private String token;
	private String name;
	private String message;
	private Integer visitCount;
	private Date expirationDate;
	private String passwordDigest;
	private Date created;
	private Date updated;
	
	public ShareBlock() { 
		this.external = false;
		this.externalUploadAllowed = false;
		this.visitCount = 0;
	}
	
	public ShareBlock(NimbusUser user) {
		this.userId = user.getId();
		this.external = false;
		this.externalUploadAllowed = false;
		this.visitCount = 0;
	}
	
	public ShareBlock(Long id, Long userId, Boolean external,
			Boolean externalUploadAllowed, String token, String name,
			String message, Integer visitCount, Date expirationDate,
			String passwordDigest, Date createDate, Date lastUpdateDate) {
		super();
		this.id = id;
		this.userId = userId;
		this.external = external;
		this.externalUploadAllowed = externalUploadAllowed;
		this.token = token;
		this.name = name;
		this.message = message;
		this.visitCount = visitCount;
		this.expirationDate = expirationDate;
		this.passwordDigest = passwordDigest;
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
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public boolean isExternal() {
		return external != null && external;
	}
	
	public void setExternal(Boolean isExternal) {
		this.external = isExternal;
	}
	
	public boolean isExternalUploadAllowed() {
		return externalUploadAllowed != null && externalUploadAllowed;
	}
	
	public void setExternalUploadAllowed(Boolean isExternalUploadAllowed) {
		this.externalUploadAllowed = isExternalUploadAllowed;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Integer getVisitCount() {
		return visitCount;
	}
	
	public void setVisitCount(Integer visitCount) {
		this.visitCount = visitCount;
	}
	
	public Date getExpirationDate() {
		return expirationDate;
	}
	
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}
	
	public String getPasswordDigest() {
		return passwordDigest;
	}
	
	public void setPasswordDigest(String passwordDigest) {
		this.passwordDigest = passwordDigest;
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
	public String getPath() {
		return getToken();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((expirationDate == null) ? 0 : expirationDate.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((external == null) ? 0 : external.hashCode());
		result = prime * result + ((externalUploadAllowed == null) ? 0 : externalUploadAllowed.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((passwordDigest == null) ? 0 : passwordDigest.hashCode());
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ShareBlock))
			return false;
		ShareBlock other = (ShareBlock) obj;
		if (expirationDate == null) {
			if (other.expirationDate != null)
				return false;
		} else if (!expirationDate.equals(other.expirationDate))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (external == null) {
			if (other.external != null)
				return false;
		} else if (!external.equals(other.external))
			return false;
		if (externalUploadAllowed == null) {
			if (other.externalUploadAllowed != null)
				return false;
		} else if (!externalUploadAllowed.equals(other.externalUploadAllowed))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (passwordDigest == null) {
			if (other.passwordDigest != null)
				return false;
		} else if (!passwordDigest.equals(other.passwordDigest))
			return false;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
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
		return "ShareBlock [id=" + id + ", userId=" + userId + ", isExternal="
				+ external + ", isExternalUploadAllowed="
				+ externalUploadAllowed + ", token=" + token + ", name="
				+ name + ", message=" + message + ", visitCount=" + visitCount
				+ ", expirationDate=" + expirationDate + ", passwordDigest="
				+ passwordDigest + "]";
	}

	@Override
	public int compareTo(ShareBlock o) {
		int i = ComparatorUtil.nullSafeDateComparator(this.created, o.created);
		i = i == 0 ? ComparatorUtil.nullSafeDateComparator(this.updated, o.updated) : i;
		return i;
	}
}
