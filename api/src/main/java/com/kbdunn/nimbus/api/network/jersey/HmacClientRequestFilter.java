package com.kbdunn.nimbus.api.network.jersey;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.api.network.util.UriUtil;

@Provider
@Priority(1) // Execute last
public class HmacClientRequestFilter implements ClientRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HmacClientRequestFilter.class.getName());
	
	public HmacClientRequestFilter() {
		super();
	}

	@Override
	public void filter(ClientRequestContext requestContext) throws IOException {
		final ObjectMapper mapper = ObjectMapperSingleton.getMapper();
		final NimbusRequest<?, ?> request = (NimbusRequest<?, ?>) requestContext.getProperty(NimbusRequest.PROPERTY_NAME);
		final NimbusHttpHeaders requestHeaders = new NimbusHttpHeaders();
		requestHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, mapper.writeValueAsString(new Date()).replace("\"", "")); // unquote
		requestHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, request.getCredentials().getUsername());
		requestHeaders.put(NimbusHttpHeaders.Key.ORIGINATION_ID, request.getOriginationId());
		
		String mac = null;
		String verb = NimbusRequest.Method.getVerbString(request.getMethod());
		String resource = requestContext.getUri().getPath();
		String content = null;
		String contentType = String.valueOf(requestContext.getHeaders().getFirst("Content-Type"));
		// Can't get the content of a file upload
		if (requestContext.getEntity() != null
				&& (contentType == null || (!contentType.contains(MediaType.APPLICATION_OCTET_STREAM) 
						&& !contentType.contains(MediaType.MULTIPART_FORM_DATA)))) { 
			content = mapper.writeValueAsString(requestContext.getEntity());
		}
		
		try {
			mac = HmacUtil.hmacDigestRequest(
					request.getCredentials().getApiToken(), 
					verb,
					resource,
					content,
					contentType,
					requestHeaders, 
					UriUtil.splitQuery(requestContext.getUri())
				);
		} catch (Exception e) {
			log.error("Error generating HMAC signature for request!", e);
		}
		
		// Set request headers
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.TIMESTAMP.toString(), requestHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.REQUESTOR.toString(), requestHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.ORIGINATION_ID.toString(), requestHeaders.get(NimbusHttpHeaders.Key.ORIGINATION_ID));
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.SIGNATURE.toString(), mac);
	}
}