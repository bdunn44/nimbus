package com.kbdunn.nimbus.common.exception;

import com.kbdunn.nimbus.common.model.NimbusUser;

public class NullEmailTransportException extends NimbusException {

	private static final long serialVersionUID = -3266222762289835509L;
	
	public NullEmailTransportException(NimbusUser user) {
		super("User '" + user.getName() + "' has not configured an email transport.");
	}
}
