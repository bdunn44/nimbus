package com.kbdunn.nimbus.web.event;

public class ShareBlockModificationEvent extends Event {

	public ShareBlockModificationEvent(Object source) {
		super(source);
	}
	
	public interface ShareBlockModificationListener extends EventListener<ShareBlockModificationEvent> {  }
}
