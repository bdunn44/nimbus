package com.kbdunn.nimbus.server.api;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.server.NimbusContext;

@Provider
@Priority(1) // Execute last
public class HmacContainerResponseFilter implements ContainerResponseFilter {

	private static final Logger log = LogManager.getLogger(HmacContainerResponseFilter.class.getName());
	
	public HmacContainerResponseFilter() {
		super();
	}
	
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
		if (Response.Status.fromStatusCode(responseContext.getStatus()).getFamily().equals(Response.Status.Family.CLIENT_ERROR)) {
			return;
		} 
		if (requestContext.getUriInfo().getRequestUri().getPath().equalsIgnoreCase("/api/application.wadl")){
			return;
		}
		
		NimbusHttpHeaders responseHeaders = new NimbusHttpHeaders();
		ObjectMapper mapper = ObjectMapperSingleton.getMapper();
		responseHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, mapper.writeValueAsString(new Date()).replace("\"", "")); // unquote
		responseHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, (String) requestContext.getProperty(HmacContainerRequestFilter.REQUEST_API_TOKEN));
		String mac = null;
		
		try {
			mac = HmacUtil.hmacDigestResponse(
					NimbusContext.instance().getUserService().getUserByApiToken(responseHeaders.get(NimbusHttpHeaders.Key.REQUESTOR)).getHmacKey(), 
					String.valueOf(responseContext.getStatus()),
					mapper.writeValueAsString(responseContext.getEntity()),
					responseContext.getStringHeaders().getFirst("Content-Type"),
					responseHeaders
				);
		} catch (Exception e) {
			log.error(e, e);
		}
		
		// Set response headers
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.TIMESTAMP.toString(), responseHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.REQUESTOR.toString(), responseHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.SIGNATURE.toString(), mac);
	}
}
