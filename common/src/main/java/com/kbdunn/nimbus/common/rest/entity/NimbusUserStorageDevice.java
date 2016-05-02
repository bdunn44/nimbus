package com.kbdunn.nimbus.common.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;

@JsonIgnoreProperties(value = {
		"user"
})
public class NimbusUserStorageDevice {
	
	private NimbusUser user;
	private StorageDevice storageDevice;
	private boolean isActivated;
	
	@JsonIgnoreProperties(value = {
			"userId",
			"storageDeviceId",
			"size",
			"lastReconciled",
			"directory",
			"song",
			"video",
			"image",
			"libraryRemoved",
			"fileExtension" 
		})
	private NimbusFile homeFolder;
	
	public NimbusUserStorageDevice(NimbusUser user, StorageDevice storageDevice) {
		this.user = user;
		this.storageDevice = storageDevice;
	}
	
	public NimbusUser getUser() {
		return user;
	}
	public void setUser(NimbusUser user) {
		this.user = user;
	}
	public StorageDevice getStorageDevice() {
		return storageDevice;
	}
	public void setStorageDevice(StorageDevice storageDevice) {
		this.storageDevice = storageDevice;
	}
	public NimbusFile getHomeFolder() {
		return homeFolder;
	}
	public void setHomeFolder(NimbusFile homeFolder) {
		this.homeFolder = homeFolder;
	}

	public boolean isActivated() {
		return isActivated;
	}

	public void setActivated(boolean isActive) {
		this.isActivated = isActive;
	}
}
