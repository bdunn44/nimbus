package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class ShareBlockComment extends Message {
	
	private Long shareBlockId;
	private Boolean anonymous;
	private String anonymousUserIpAddress;
	
	public ShareBlockComment(Long id, Long userId, String message, Long shareBlockId,
			Boolean anonymous, String anonymousUserIpAddress, Date createDate, Date lastUpdateDate) {
		super(id, userId, message, createDate, lastUpdateDate);
		this.shareBlockId = shareBlockId;
		this.anonymous = anonymous;
		this.anonymousUserIpAddress = anonymousUserIpAddress;
	}
	
	public Long getShareBlockId() {
		return shareBlockId;
	}
	
	public void setShareBlockId(Long shareBlockId) {
		this.shareBlockId = shareBlockId;
	}
	
	public boolean getIsAnonymous() {
		return anonymous != null && anonymous;
	}
	
	public void setIsAnonymous(Boolean isAnonymous) {
		this.anonymous = isAnonymous;
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
				+ ", isAnonymous=" + anonymous + ", anonymousUserIpAddress="
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
				+ ((anonymous == null) ? 0 : anonymous.hashCode());
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
		if (anonymous == null) {
			if (other.anonymous != null)
				return false;
		} else if (!anonymous.equals(other.anonymous))
			return false;
		if (shareBlockId == null) {
			if (other.shareBlockId != null)
				return false;
		} else if (!shareBlockId.equals(other.shareBlockId))
			return false;
		return true;
	}
}
