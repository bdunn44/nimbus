package com.kbdunn.nimbus.server.api;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.common.model.NimbusUser;
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
		/*if (Response.Status.fromStatusCode(responseContext.getStatus()).getFamily().equals(Response.Status.Family.CLIENT_ERROR)) {
			return;
		} */
		if (requestContext.getUriInfo().getRequestUri().getPath().toLowerCase().endsWith("/application.wadl")){
			return;
		}
		
		NimbusHttpHeaders responseHeaders = new NimbusHttpHeaders();
		ObjectMapper mapper = ObjectMapperSingleton.getMapper();
		responseHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, mapper.writeValueAsString(new Date()).replace("\"", "")); // unquote
		responseHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, (String) requestContext.getProperty(NimbusHttpHeaders.Key.REQUESTOR));
		if (requestContext.getProperty(NimbusHttpHeaders.Key.ORIGINATION_ID) != null) {
			responseHeaders.put(NimbusHttpHeaders.Key.ORIGINATION_ID, (String) requestContext.getProperty(NimbusHttpHeaders.Key.ORIGINATION_ID));
		}
		
		// Set response headers
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.TIMESTAMP.toString(), responseHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.REQUESTOR.toString(), responseHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		if (responseHeaders.containsKey(NimbusHttpHeaders.Key.ORIGINATION_ID)) {
			responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.ORIGINATION_ID.toString(), responseHeaders.get(NimbusHttpHeaders.Key.ORIGINATION_ID));
		}
		
		String mac = null;
		String contentType = responseContext.getStringHeaders().getFirst("Content-Type");
		
		try {
			String token =  responseHeaders.get(NimbusHttpHeaders.Key.REQUESTOR);
			if (token == null || token.isEmpty()) throw new IllegalStateException("API token property is unset for request context");
			NimbusUser user = NimbusContext.instance().getUserService().getUserByApiToken(token);
			if (user == null) throw new IllegalArgumentException("Unable to find user with API token '" + token + "'");
			String entity = null;
			if (responseContext.getEntityStream() != null
				&& (contentType == null || (!contentType.contains(MediaType.APPLICATION_OCTET_STREAM) 
						&& !contentType.contains(MediaType.MULTIPART_FORM_DATA)))) {
				entity = mapper.writeValueAsString(responseContext.getEntity());
			}
			mac = HmacUtil.hmacDigestResponse(
					user.getHmacKey(), 
					String.valueOf(responseContext.getStatus()),
					entity,
					contentType,
					responseHeaders
				);
		} catch (Exception e) {
			log.error("Error calculating response HMAC", e);
		}
		
		// Set response signature
		responseContext.getHeaders().putSingle(NimbusHttpHeaders.Key.SIGNATURE.toString(), mac);
	}
}
