package com.kbdunn.nimbus.server.api.async;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;

public class AtmosphereHmacInterceptor implements AtmosphereInterceptor {//, InvokationOrder {

	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(AtmosphereHmacInterceptor.class);

	// TODO: This entire class
	
	/*@Override
	public PRIORITY priority() {
		return PRIORITY.FIRST_BEFORE_DEFAULT;
	}*/
	
	@Override
	public void configure(AtmosphereConfig config) {
		//log.info("Configure called");
	}

	@Override
	public Action inspect(AtmosphereResource r) {
		//log.info("Inspect called");
		return Action.CONTINUE;
	}

	@Override
	public void postInspect(AtmosphereResource r) {
		//log.info("Post inspect called");
	}
}
