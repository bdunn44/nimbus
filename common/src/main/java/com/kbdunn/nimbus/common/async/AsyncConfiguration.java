package com.kbdunn.nimbus.common.async;

public class AsyncConfiguration {
	
	private String name;
	private long notifyInterval = 2000L;
	private float notifyProgressInterval = .05f;
	
	public AsyncConfiguration() {
		
	}
	
	public AsyncConfiguration(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public long getNotifyInterval() {
		return notifyInterval;
	}
	
	public void setNotifyInterval(long milis) {
		notifyInterval = milis;
	}
	
	public float getNotifyProgressInterval() {
		return notifyProgressInterval;
	}
	
	public void setNotifyProgressInterval(float notifyProgressInterval) {
		this.notifyProgressInterval = notifyProgressInterval;
	}
}
