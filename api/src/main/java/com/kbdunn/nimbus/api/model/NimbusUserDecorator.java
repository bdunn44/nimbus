package com.kbdunn.nimbus.api.model;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.model.NimbusUser;

public class NimbusUserDecorator extends NimbusUser {
	
	private List<NimbusUserStorageDevice> storageDevices;

	public NimbusUserDecorator(NimbusUser user) {
		super(user);
	}
	
	public List<NimbusUserStorageDevice> getStorageDevices() {
		return storageDevices;
	}

	public void setStorageDevices(List<NimbusUserStorageDevice> storageDevices) {
		this.storageDevices = storageDevices;
	}
	
	public void addStorageDevice(NimbusUserStorageDevice storageDevice) {
		if (storageDevices == null) storageDevices = new ArrayList<>();
		storageDevices.add(storageDevice);
	}
}