package com.kbdunn.nimbus.server.api.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;
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
	private static final ExecutorService pushExecutor = Executors.newSingleThreadExecutor();
	
	@Ready
    public void onReady(final AtmosphereResource r) {
		log.info("Client connected " + r.uuid());
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
    
    public static boolean isUserConnected(NimbusUser user) {
    	if (user.getApiToken() == null) throw new IllegalArgumentException("User's API Token cannot be null");
    	return clientMap.containsKey(user.getApiToken());
    }
    
    public static void pushFileEvent(NimbusUser user, FileEvent event) {
    	if (!isUserConnected(user)) return;
		final AtmosphereResource resource = clientMap.get(user.getApiToken());
		/*if (resource.getBroadcaster().isDestroyed()) {
			log.warn("Broadcaster unexpectedly destroyed! " + resource.getBroadcaster());
			clientMap.remove(user.getApiToken());
			return;
		}*/
    	pushExecutor.submit(() -> {
			try {
				resource.write(ObjectMapperSingleton.getMapper().writeValueAsBytes(event));
				log.debug("File event pushed to user " + user + ": " + event);
			} catch (JsonProcessingException e) {
				log.error("Error pushing file event to user", e);
			}
    	});
    }
}
