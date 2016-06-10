package com.kbdunn.nimbus.server.api.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Ready;
import org.atmosphere.config.service.Singleton;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.IdleResourceInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.SyncRootChangeEvent;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.common.model.NimbusUser;

@Singleton
@ManagedService(path = "/async/eventbus", 
	interceptors = {
            AtmosphereResourceLifecycleInterceptor.class,
            TrackMessageSizeInterceptor.class,
            IdleResourceInterceptor.class,
            SuspendTrackerInterceptor.class,
            AtmosphereHmacInterceptor.class})
public class EventBus {
	
	private static final Logger log = LogManager.getLogger(EventBus.class);
	private static final ConcurrentHashMap<String, AtmosphereResource> clientMap = new ConcurrentHashMap<>();
	
	@Ready
    public void onReady(final AtmosphereResource r) {
		log.info("Client connected " + r.uuid() + " (" + r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR) + ")");
		clientMap.put(r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR), r);
		r.suspend();
	}
	
    @Disconnect
    public void onDisconnect(AtmosphereResourceEvent event) {
    	if (event.isCancelled()) { 
    		log.info("Client unexpectedly disconnected. " + event.getResource().uuid());
    	} else if (event.isClosedByClient()) {
    		log.info("Client closed the connection. " + event.getResource().uuid());
    	}
		for (Map.Entry<String, AtmosphereResource> e : clientMap.entrySet()) {
			if (e.getValue().equals(event.getResource())) {
				clientMap.remove(e.getKey());
			}
		}
    }
    
    public static boolean userIsConnected(NimbusUser user) {
    	log.info("Checking if " + user.getName() + " is connected. " + (clientMap.containsKey(user.getName())
			|| clientMap.containsKey(user.getEmail())));
    	return clientMap.containsKey(user.getName())
    			|| clientMap.containsKey(user.getEmail());
    }
    
    public static void pushFileEvent(NimbusUser user, FileEvent event) {
    	publish(user, event);
    }
    
    public static void pushRootChangeEvent(NimbusUser user, SyncRootChangeEvent event) {
    	publish(user, event);
    }
    
    private static void publish(NimbusUser user, Object message) {
    	if (userIsConnected(user)) {
			final AtmosphereResource resource = 
					clientMap.containsKey(user.getName()) ? clientMap.get(user.getName()) :
					clientMap.containsKey(user.getEmail()) ? clientMap.get(user.getEmail()) :
					null;
			if (resource == null) {
				log.warn("Attempted to push a file event to user that is no longer connected.");
				return;
			}
			/*if (resource.getBroadcaster().isDestroyed()) {
				log.warn("Broadcaster unexpectedly destroyed! " + resource.getBroadcaster());
				clientMap.remove(user.getApiToken());
				return;
			}*/
			try {
				resource.write(ObjectMapperSingleton.getMapper().writeValueAsBytes(message));
				log.debug("Message pushed to user " + user + ": " + message);
			} catch (JsonProcessingException e) {
				log.error("Error pushing message to user", e);
			}
    	}
    }
}
