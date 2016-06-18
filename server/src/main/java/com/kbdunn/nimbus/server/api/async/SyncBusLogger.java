package com.kbdunn.nimbus.server.api.async;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterListener;
import org.atmosphere.cpr.Deliver;

public class SyncBusLogger implements BroadcasterListener {

	private static final Logger log = LogManager.getLogger(SyncBusLogger.class);

	@Override
	public void onPostCreate(Broadcaster b) {
		log.info("onPostCreate() " + b.getID());
	}

	@Override
	public void onComplete(Broadcaster b) {
		log.info("onComplete() " + b.getID());
	}

	@Override
	public void onPreDestroy(Broadcaster b) {
		log.info("onPreDestroy() " + b.getID());
	}

	@Override
	public void onAddAtmosphereResource(Broadcaster b, AtmosphereResource r) {
		log.info("onAddAtmosphereResource() " + r.uuid());
	}

	@Override
	public void onRemoveAtmosphereResource(Broadcaster b, AtmosphereResource r) {
		log.info("onRemoveAtmosphereResource() " + r.uuid());
	}

	@Override
	public void onMessage(Broadcaster b, Deliver deliver) {
		log.info("onMessage() " + deliver.getMessage());
	}
}
