package com.kbdunn.nimbus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=As.PROPERTY, property="@type")
public interface StorageDevice extends NimbusRecord, Comparable<StorageDevice>, FileContainer {
	void setName(String name);
	void setPath(String path);
	String getType();
	boolean isReconciled();
	void setReconciled(boolean isReconciled);
	boolean isAutonomous();
	void setAutonomous(boolean isAutonomous);
}
