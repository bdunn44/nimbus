package com.kbdunn.nimbus.web.event;

import com.kbdunn.nimbus.common.model.NimbusUser;

public class HardDriveModificationEvent extends Event {

	private NimbusUser affectedUser;
	
	public HardDriveModificationEvent(Object source, NimbusUser affectedUser) {
		super(source);
		this.affectedUser = affectedUser;
	}
	
	public NimbusUser geAtffectedUser() {
		return affectedUser;
	}
	
	public interface HardDriveModificationListener extends EventListener<HardDriveModificationEvent> {  }
}