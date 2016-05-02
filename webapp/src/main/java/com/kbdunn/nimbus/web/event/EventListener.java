package com.kbdunn.nimbus.web.event;

public interface EventListener<T extends Event> {
	void notifyEvent(T event);
}
