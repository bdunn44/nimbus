package com.kbdunn.nimbus.common.model;

import java.util.Date;

public class UserMessage extends Message {

	private Long recipientUserId;
	
	public UserMessage(Long id, Long userId, String message, Long recipientUserId, Date createDate, Date lastUpdateDate) {
		super(id, userId, message, createDate, lastUpdateDate);
		this.recipientUserId = recipientUserId;
	}

	public Long getRecipientUserId() {
		return recipientUserId;
	}
	
	public void setRecipientUserId(Long recipientUserId) {
		this.recipientUserId = recipientUserId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((recipientUserId == null) ? 0 : recipientUserId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof UserMessage))
			return false;
		UserMessage other = (UserMessage) obj;
		if (recipientUserId == null) {
			if (other.recipientUserId != null)
				return false;
		} else if (!recipientUserId.equals(other.recipientUserId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UserMessage [recipientUserId=" + recipientUserId + "]";
	}
}
