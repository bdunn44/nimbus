package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class ShareBlockComment extends Message {
	
	private Long shareBlockId;
	private Boolean isAnonymous;
	private String anonymousUserIpAddress;
	
	public ShareBlockComment(Long id, Long userId, String message, Long shareBlockId,
			Boolean isAnonymous, String anonymousUserIpAddress, Date createDate, Date lastUpdateDate) {
		super(id, userId, message, createDate, lastUpdateDate);
		this.shareBlockId = shareBlockId;
		this.isAnonymous = isAnonymous;
		this.anonymousUserIpAddress = anonymousUserIpAddress;
	}
	
	public Long getShareBlockId() {
		return shareBlockId;
	}
	
	public void setShareBlockId(Long shareBlockId) {
		this.shareBlockId = shareBlockId;
	}
	
	public boolean getIsAnonymous() {
		return isAnonymous != null && isAnonymous;
	}
	
	public void setIsAnonymous(Boolean isAnonymous) {
		this.isAnonymous = isAnonymous;
	}
	
	public String getAnonymousUserIpAddress() {
		return anonymousUserIpAddress;
	}
	
	public void setAnonymousUserIpAddress(String anonymousUserIpAddress) {
		this.anonymousUserIpAddress = anonymousUserIpAddress;
	}

	@Override
	public String toString() {
		return "ShareBlockComment [shareBlockId=" + shareBlockId
				+ ", isAnonymous=" + isAnonymous + ", anonymousUserIpAddress="
				+ anonymousUserIpAddress + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime
				* result
				+ ((anonymousUserIpAddress == null) ? 0
						: anonymousUserIpAddress.hashCode());
		result = prime * result
				+ ((isAnonymous == null) ? 0 : isAnonymous.hashCode());
		result = prime * result
				+ ((shareBlockId == null) ? 0 : shareBlockId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof ShareBlockComment))
			return false;
		ShareBlockComment other = (ShareBlockComment) obj;
		if (anonymousUserIpAddress == null) {
			if (other.anonymousUserIpAddress != null)
				return false;
		} else if (!anonymousUserIpAddress.equals(other.anonymousUserIpAddress))
			return false;
		if (isAnonymous == null) {
			if (other.isAnonymous != null)
				return false;
		} else if (!isAnonymous.equals(other.isAnonymous))
			return false;
		if (shareBlockId == null) {
			if (other.shareBlockId != null)
				return false;
		} else if (!shareBlockId.equals(other.shareBlockId))
			return false;
		return true;
	}
}
