package com.kbdunn.nimbus.server.api.async;

import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Message;
import org.atmosphere.config.service.Ready;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.common.sync.interfaces.FileEvent;

@ManagedService(path = "/syncbus")
public class SyncEventBus {

	private static final Logger log = LoggerFactory.getLogger(SyncEventBus.class);
	
	@Ready
    public void onReady(final AtmosphereResource r) {
		log.info("Browser {} connected", r.uuid());
	}
	
    @Disconnect
    public void onDisconnect(AtmosphereResourceEvent event) {
    	if (event.isCancelled()) {
    		log.info("Browser {} unexpectedly disconnected.", event.getResource().uuid());
    	} else if (event.isClosedByClient()) {
    		log.info("Browser {} closed the connection.", event.getResource().uuid());
    	}
    }
    
   /*@Message(encoders=JacksonEncoder.class)
    public void onMessage(FileEvent event) {
    	log.info("Atmosphere is processing a message for FileEvent {}", event);
    }*/
    
    @Message
    public void onMessage(String event) {
    	log.info("Atmosphere is processing a message for FileEvent {}", event);
    }
}
