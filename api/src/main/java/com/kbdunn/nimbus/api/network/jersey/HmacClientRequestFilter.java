package com.kbdunn.nimbus.api.network.jersey;

import java.io.IOException;
import java.util.Date;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
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
		final NimbusRequest<?> request = (NimbusRequest<?>) requestContext.getProperty(NimbusRequest.PROPERTY_NAME);
		final NimbusHttpHeaders requestHeaders = new NimbusHttpHeaders();
		requestHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, mapper.writeValueAsString(new Date()).replace("\"", "")); // unquote
		requestHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, request.getCredentials().getApiToken());
		
		String mac = null;
		String verb = NimbusRequest.Method.getVerbString(request.getMethod());
		String resource = requestContext.getUri().getPath();
		String content = mapper.writeValueAsString(requestContext.getEntity());
		String contentType = (String) requestContext.getHeaders().getFirst("Content-Type");
		
		try {
			mac = HmacUtil.hmacDigestRequest(
					request.getCredentials().getHmacKey(), 
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
		
		// Set response headers
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.TIMESTAMP.toString(), requestHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.REQUESTOR.toString(), requestHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		requestContext.getHeaders().putSingle(NimbusHttpHeaders.Key.SIGNATURE.toString(), mac);
	}
}