package com.kbdunn.nimbus.server.api.async;

import java.util.Date;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.atmosphere.cpr.Action;
import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.util.DateUtil;
import com.kbdunn.nimbus.server.NimbusContext;

public class AtmosphereHmacInterceptor implements AtmosphereInterceptor {

	private static final Logger log = LogManager.getLogger(AtmosphereHmacInterceptor.class);
	
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
		// TODO: Validate HMAC signature - also sign responses?
		log.info("Inpect called!");
		log.info("  Request: " +  r.getRequest());
		log.info("  Response: " +  r.getResponse());
		// Validate request signature
		String verb = "GET";
		String resource = r.getRequest().getRequestURI();
		String requestor = r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR);
		String ts = r.getRequest().getHeader(NimbusHttpHeaders.Key.TIMESTAMP);
		String mac = r.getRequest().getHeader(NimbusHttpHeaders.Key.SIGNATURE);
		
		log.debug("Inbound PUSH Request:");
		log.debug("    verb: " + verb);
		log.debug("    resource: " + resource);
		log.debug("    requestor: " + requestor);
		log.debug("    timestamp: " + ts);
		log.debug("    signature: " + mac);
		
		try {
			// Check required parameters are present
			if (verb == null
					|| resource == null || resource.isEmpty() || "null".equals(resource)
					|| requestor == null || requestor.isEmpty() || "null".equals(requestor)
					|| ts == null || ts.isEmpty() || "null".equals(ts)
					|| mac == null || mac.isEmpty() || "null".equals(mac)) {
				devModeOrThrowIae("Invalid request - verb, requestor, timestamp, or signature not present.");
			}
			
			// Check valid requestor
			NimbusUser user = NimbusContext.instance().getUserService().getUserByNameOrEmail(requestor);
			if (user == null) {
				throw new IllegalArgumentException("Invalid requestor! (" + requestor + ")");
			}
			
			// Check for a stale timestamp
			DateTimeFormatter parser = DateTimeFormat.forPattern(DateUtil.DATE_FORMAT).withZoneUTC();
			DateTime timestamp = parser.parseDateTime(ts);
			if (timestamp.compareTo(DateTime.now().minusMinutes(5)) == -1) {
				devModeOrThrowIae("Stale message!  (" + parser.print(timestamp) + ")");
			}	
			NimbusHttpHeaders nHeaders = new NimbusHttpHeaders();
			nHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, requestor);
			nHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, ts);
			String validMac;
			validMac = HmacUtil.hmacDigestRequest(
					user.getApiToken(), 
					verb, 
					resource, 
					null, 
					null, 
					nHeaders, 
					null
				);
			
			if (!mac.equals(validMac)) {
				if (NimbusContext.instance().getPropertiesService().isDevMode()) log.debug("Valid signature is '" + validMac + "'");
				devModeOrThrowIae("Invalid signature!");
			}
		} catch (Exception e) {
			log.error("Aborting PUSH request", e);
			return Action.CANCELLED;
		}
		return Action.CONTINUE;
	}

	@Override
	public void postInspect(AtmosphereResource r) {
		try {
			ObjectMapper mapper = ObjectMapperSingleton.getMapper();
			NimbusHttpHeaders nHeaders = new NimbusHttpHeaders();
			nHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, r.getRequest().getHeader(NimbusHttpHeaders.Key.REQUESTOR));
			nHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, mapper.writeValueAsString(new Date()).replace("\"", "")); // unquote
			NimbusUser user = NimbusContext.instance().getUserService().getUserByNameOrEmail(nHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
			
			r.getResponse().addHeader(NimbusHttpHeaders.Key.REQUESTOR, nHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
			r.getResponse().addHeader(NimbusHttpHeaders.Key.TIMESTAMP, nHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
			r.getResponse().addHeader(NimbusHttpHeaders.Key.SIGNATURE, HmacUtil.hmacDigestResponse(
					user.getApiToken(), 
					String.valueOf(r.getResponse().getStatus()), 
					null, 
					r.getResponse().getContentType(), 
					nHeaders
				));
		} catch (Exception e) {
			log.error("Error calculating response HMAC", e);
		}
		
		log.info("Post-Inpect called!");
		log.info("  Request: " +  r.getRequest());
		log.info("  Response: " +  r.getResponse());
	}
	
	private void devModeOrThrowIae(String msg) throws Exception {
		log.error("Bad PUSH Request! " + msg);
		if (!NimbusContext.instance().getPropertiesService().isDevMode()) {
			log.debug("Throwing exception");
			throw new IllegalArgumentException(msg);
		}
	}
}
