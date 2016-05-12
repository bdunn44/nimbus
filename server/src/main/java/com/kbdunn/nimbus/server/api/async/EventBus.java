package com.kbdunn.nimbus.server.api.async;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.service.Disconnect;
import org.atmosphere.config.service.ManagedService;
import org.atmosphere.config.service.Post;
import org.atmosphere.config.service.Ready;
import org.atmosphere.config.service.Singleton;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.IdleResourceInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kbdunn.nimbus.api.client.model.FileAddEvent;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.server.NimbusContext;

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
	
	private static ConcurrentHashMap<String, AtmosphereResource> clientMap = new ConcurrentHashMap<>();
	
	@Ready
    public void onReady(final AtmosphereResource r) {
		log.info("Client connected " + r.uuid());
		clientMap.put(r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR), r);
		r.suspend();
		pushFileEvent(
				NimbusContext.instance().getUserService().getUserByApiToken(r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR)),
				new FileAddEvent(new SyncFile("/rootshit", null, true))
			);
	}
	
    @Disconnect
    public void onDisconnect(AtmosphereResourceEvent event) {
    	if (event.isCancelled()) { 
    		log.info("Browser unexpectedly disconnected. " + event.getResource().uuid());
    	} else if (event.isClosedByClient()) {
    		log.info("Browser closed the connection. " + event.getResource().uuid());
    	}
		for (Map.Entry<String, AtmosphereResource> e : clientMap.entrySet()) {
			if (e.getValue().equals(event.getResource())) {
				clientMap.remove(e.getKey());
			}
		}
    }
    
    @Post
    public void onPost(AtmosphereResource r) {
    	// Don't think I need this.. Events coming from the client can use the REST API
    	log.info("Message posted from " + r.uuid() + ". Content: " + r.getRequest().body().asString());
    }
    
    public static boolean isUserConnected(NimbusUser user) {
    	if (user.getApiToken() == null) throw new IllegalArgumentException("User's API Token cannot be null");
    	return clientMap.containsKey(user.getApiToken());
    }
    
    public static void pushFileEvent(NimbusUser user, FileEvent event) {
    	if (!isUserConnected(user)) return;
		final AtmosphereResource resource = clientMap.get(user.getApiToken());
		if (resource.getBroadcaster().isDestroyed()) {
			log.warn("Broadcaster unexpectedly destroyed! " + resource.getBroadcaster());
			clientMap.remove(user.getApiToken());
			return;
		}
		try {
			resource.write(ObjectMapperSingleton.getMapper().writeValueAsBytes(event));
			log.debug("File event pushed to user " + user);
		} catch (JsonProcessingException e) {
			log.error("Error pushing file event to user", e);
		}
    }
}
