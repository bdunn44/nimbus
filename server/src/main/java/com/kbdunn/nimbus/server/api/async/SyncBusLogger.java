package com.kbdunn.nimbus.server.api.async;

import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.websocket.WebSocketEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("rawtypes")
public class SyncBusLogger implements WebSocketEventListener {

	private static final Logger log = LoggerFactory.getLogger(SyncEventBus.class);
	
	@Override
    public void onPreSuspend(AtmosphereResourceEvent event) {
		
    }

	@Override
    public void onSuspend(final AtmosphereResourceEvent event) {
        log.info("onSuspend(): {}:{}", event.getResource().getRequest().getRemoteAddr(),
                event.getResource().getRequest().getRemotePort());
    }

	@Override
    public void onResume(AtmosphereResourceEvent event) {
        log.info("onResume(): {}:{}", event.getResource().getRequest().getRemoteAddr(),
                event.getResource().getRequest().getRemotePort());
    }

	@Override
    public void onDisconnect(AtmosphereResourceEvent event) {
        log.info("onDisconnect(): {}:{}", event.getResource().getRequest().getRemoteAddr(),
                event.getResource().getRequest().getRemotePort());
    }

	@Override
    public void onBroadcast(AtmosphereResourceEvent event) {
        log.info("onBroadcast(): {}", event.getMessage());
    }

	@Override
    public void onHeartbeat(AtmosphereResourceEvent event) {
        log.info("onHeartbeat(): {}", event.getMessage());
    }

	@Override
    public void onThrowable(AtmosphereResourceEvent event) {
        log.warn("onThrowable(): {}", event);
    }

    @Override
    public void onClose(AtmosphereResourceEvent event) {
        log.info("onClose(): {}", event.getMessage());

    }

	@Override
    public void onHandshake(WebSocketEvent event) {
        log.info("onHandshake(): {}", event);
    }

	@Override
    public void onMessage(WebSocketEvent event) {
        log.info("onMessage(): {}", event);
    }

	@Override
    public void onClose(WebSocketEvent event) {
        log.info("onClose(): {}", event);
    }

	@Override
    public void onControl(WebSocketEvent event) {
        log.info("onControl(): {}", event);
    }

	@Override
    public void onDisconnect(WebSocketEvent event) {
        log.info("onDisconnect(): {}", event);
    }

	@Override
	public void onConnect(WebSocketEvent event) {
        log.info("onConnect(): {}", event);
	}
}
