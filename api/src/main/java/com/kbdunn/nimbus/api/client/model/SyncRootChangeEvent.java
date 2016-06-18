package com.kbdunn.nimbus.api.client.model;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;

public class SyncRootChangeEvent extends SyncEvent {

	private NimbusUser user;
	private StorageDevice oldRoot;
	private StorageDevice newRoot;
	
	public SyncRootChangeEvent() {  }
	
	public SyncRootChangeEvent(NimbusUser user, StorageDevice oldRoot, StorageDevice newRoot) {
		super();
		this.user = user;
		this.oldRoot = oldRoot;
		this.newRoot = newRoot;
	}

	public NimbusUser getUser() {
		return user;
	}
	
	public void setUser(NimbusUser user) {
		this.user = user;
	}
	
	public StorageDevice getOldRoot() {
		return oldRoot;
	}
	
	public void setOldRoot(StorageDevice oldRoot) {
		this.oldRoot = oldRoot;
	}
	
	public StorageDevice getNewRoot() {
		return newRoot;
	}
	
	public void setNewRoot(StorageDevice newRoot) {
		this.newRoot = newRoot;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((newRoot == null) ? 0 : newRoot.hashCode());
		result = prime * result + ((oldRoot == null) ? 0 : oldRoot.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SyncRootChangeEvent))
			return false;
		SyncRootChangeEvent other = (SyncRootChangeEvent) obj;
		if (newRoot == null) {
			if (other.newRoot != null)
				return false;
		} else if (!newRoot.equals(other.newRoot))
			return false;
		if (oldRoot == null) {
			if (other.oldRoot != null)
				return false;
		} else if (!oldRoot.equals(other.oldRoot))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SyncRootChangeEvent [user.name=" + (user == null ? "unset" : user.getName())
			+ ", oldRoot.name=" + (oldRoot == null ? "unset" : oldRoot.getName()) 
			+ ", newRoot.name=" + (newRoot == null ? "unset" : newRoot.getName()) + "]";
	}
}
