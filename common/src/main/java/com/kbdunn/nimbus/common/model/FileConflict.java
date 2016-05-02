package com.kbdunn.nimbus.common.model;

public class FileConflict {
	
	public enum Resolution { IGNORE(), REPLACE(), COPY(); }
	
	private NimbusFile source;
	private NimbusFile target;
	private Resolution resolution;
	
	public FileConflict(NimbusFile source, NimbusFile target) {
		this.source = source;
		this.target = target;
	}
	
	public NimbusFile getSource() {
		return source;
	}
	
	public NimbusFile getTarget() {
		return target;
	}
	
	public void setResolution(Resolution resolution) {
		this.resolution = resolution;
	}
	
	public Resolution getResolution() {
		return resolution;
	}
}
