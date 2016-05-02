package com.kbdunn.nimbus.web.event;

import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.web.event.HardDriveModificationEvent.HardDriveModificationListener;
import com.kbdunn.nimbus.web.event.ShareBlockModificationEvent.ShareBlockModificationListener;

public class EventRouter {
	
	public EventRouter() {  }
	
	private List<HardDriveModificationListener> hardDriveModificationListeners = new ArrayList<>();
	private List<ShareBlockModificationListener> shareBlockModificationListeners = new ArrayList<>();
	
	public void addHardDriveModificationListener(HardDriveModificationListener listener) {
		hardDriveModificationListeners.add(listener);
	}
	
	public void removeHardDriveModificationListener(HardDriveModificationListener listener) {
		hardDriveModificationListeners.remove(listener);
	}
	
	public void publishHardDriveModificationEvent(HardDriveModificationEvent event) {
		for (HardDriveModificationListener listener : hardDriveModificationListeners)
			listener.notifyEvent(event);
	}
	public void addShareBlockModificationListener(ShareBlockModificationListener listener) {
		shareBlockModificationListeners.add(listener);
	}
	
	public void removeShareBlockModificationListener(ShareBlockModificationListener listener) {
		shareBlockModificationListeners.remove(listener);
	}
	
	public void publishShareBlockModificationEvent(ShareBlockModificationEvent event) {
		for (ShareBlockModificationListener listener : shareBlockModificationListeners)
			listener.notifyEvent(event);
	}
}
