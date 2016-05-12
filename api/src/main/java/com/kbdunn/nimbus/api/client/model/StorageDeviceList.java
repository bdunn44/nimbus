package com.kbdunn.nimbus.api.client.model;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.model.StorageDevice;

public class StorageDeviceList extends ArrayList<StorageDevice> {

	private static final long serialVersionUID = 514677658330779405L;
	
	public StorageDeviceList(List<StorageDevice> source) {
		super(source);
	}
}
