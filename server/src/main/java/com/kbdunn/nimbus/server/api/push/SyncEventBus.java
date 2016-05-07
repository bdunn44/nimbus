package com.kbdunn.nimbus.server.api.push;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.config.service.ManagedService;

@ManagedService(path = "/push/sync/{userId: [0-9]+}")
public class SyncEventBus {

	private static final Logger log = LogManager.getLogger(SyncEventBus.class);
	
	
	
}
