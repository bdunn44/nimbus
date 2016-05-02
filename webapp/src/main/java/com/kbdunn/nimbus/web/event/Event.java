package com.kbdunn.nimbus.web.event;

public abstract class Event {
	
	private Object source;
	
	public Event(Object source) {
		this.source = source;
	}
	
	public Object getSource() {
		return source;
	}
}