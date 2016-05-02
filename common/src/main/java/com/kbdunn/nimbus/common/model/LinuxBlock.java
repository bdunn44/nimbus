package com.kbdunn.nimbus.common.model;

public class LinuxBlock {
	private String path, uuid, label, type;
	
	public void setPath(String path) {
		this.path = path;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getType() {
		return type;
	}
}