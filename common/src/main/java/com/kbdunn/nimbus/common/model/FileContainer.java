package com.kbdunn.nimbus.common.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

/* Two implementers of this interface: NimbusFile (folders) and ShareBlock (contains shared files) */
@JsonTypeInfo(use=JsonTypeInfo.Id.MINIMAL_CLASS, include=As.PROPERTY)
public interface FileContainer {
	public String getPath();
	public String getName();
}