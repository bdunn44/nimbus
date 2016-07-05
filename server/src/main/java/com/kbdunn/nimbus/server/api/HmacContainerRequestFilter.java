package com.kbdunn.nimbus.server.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Priority;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.message.internal.ReaderWriter;

import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.util.DateUtil;
import com.kbdunn.nimbus.server.NimbusContext;

@Provider
@Priority(1) // Execute first
@PreMatching
public class HmacContainerRequestFilter implements ContainerRequestFilter {

	private static final Logger log = LogManager.getLogger(HmacContainerRequestFilter.class.getName());
	
	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		log.debug("In HmacContainerRequestFilter");
		Map<String, String> headers = toMap(requestContext.getHeaders());
		Map<String, String> parameters = toMap(requestContext.getUriInfo().getQueryParameters(true));
		
		// Extract custom HTTP headers, coalesce with headers passed as query parameters
		NimbusHttpHeaders nmbHeaders = NimbusHttpHeaders.fromMap(headers).coalesce(parameters);
		if (nmbHeaders.containsKey(NimbusHttpHeaders.Key.ORIGINATION_ID)) {
			requestContext.setProperty(NimbusHttpHeaders.Key.ORIGINATION_ID, nmbHeaders.get(NimbusHttpHeaders.Key.ORIGINATION_ID));
		}
		
		// Remove custom headers from query parameters (if present)
		Map<String, String> nonHeaderParameters = new HashMap<>();
		for (Entry<String, String> e : parameters.entrySet()) {
			if (!e.getKey().startsWith(NimbusHttpHeaders.Key.PREFIX)) {
				nonHeaderParameters.put(e.getKey(), e.getValue());
				log.debug("Adding non-header parameter " + e.getKey() + "=" + e.getValue());
			}
		}
		
		String verb = requestContext.getMethod();
		String resource = requestContext.getUriInfo().getRequestUri().getPath();
		String content = null;
		String contentType = null;
		
		if (resource.toLowerCase().endsWith("/application.wadl")) {
			if (!NimbusContext.instance().getPropertiesService().isDevMode()) {
				requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
			} else {
				return;
			}
		}
		
		if (HttpMethod.POST.equals(requestContext.getMethod()) || HttpMethod.PUT.equals(requestContext.getMethod())) {
			contentType = requestContext.getHeaderString("Content-Type");
			
			// Multipart requests (at least with the jersey client) add boundary info to Content-Type
			// ... which is not accessible at the point of HMAC generation on the client, so we remove it here for MAC validation.
			if (contentType.startsWith(MediaType.MULTIPART_FORM_DATA)) 
				contentType = MediaType.MULTIPART_FORM_DATA;
			
			// Read entity, reset the input stream
			// Don't read entity for file uploads
			if (requestContext.getEntityStream() != null
					&& (contentType == null || (!contentType.contains(MediaType.APPLICATION_OCTET_STREAM) 
							&& !contentType.contains(MediaType.MULTIPART_FORM_DATA)))) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				InputStream in = requestContext.getEntityStream();
				try {
					if (in.available() > 0) {
						ReaderWriter.writeTo(in, out);
						byte[] entity = out.toByteArray();
						content = new String(entity);
						requestContext.setEntityStream(new ByteArrayInputStream(entity));
					}
				} catch(IOException e) {
					log.error("Error reading request body", e);
					requestContext.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
					return;
				}
			}
		} 
		
		log.debug("Inbound API Request:");
		log.debug("    verb: " + verb);
		log.debug("    resource: " + resource);
		log.debug("    requestor: " + nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		log.debug("    timestamp: " + nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		log.debug("    signature: " + nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE));
		log.debug("    origination: " + nmbHeaders.get(NimbusHttpHeaders.Key.ORIGINATION_ID));
		log.debug("    content: " + content);
		log.debug("    content type: " + contentType);
		
		try {
			// Check required parameters are present
			if (verb == null
					|| resource == null
					|| nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR) == null 
					|| nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP) == null 
					|| nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE) == null) {
				devModeOrThrowIae("Invalid request - verb, requestor, timestamp, or signature not present.");
			}
			
			// Check requestor exists
			NimbusUser requestor = NimbusContext.instance().getUserService().getUserByNameOrEmail(nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
			if (requestor == null) 
				throw new IllegalArgumentException("Invalid Requestor (" + nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR) + ")!");
			requestContext.setProperty(NimbusHttpHeaders.Key.REQUESTOR, nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
			
			// Check timestamp isn't old
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtil.JAVA8_DATE_FORMAT);
			ZonedDateTime datetime = ZonedDateTime.parse(nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP), formatter);
			if (datetime.compareTo(ZonedDateTime.now().minusMinutes(5)) == -1) {
				throw new IllegalArgumentException("Stale response!  (" + nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP) + ")");
			}
			
			// Check mac hash
			String serverMac = HmacUtil.hmacDigestRequest(requestor.getApiToken(), verb, resource, content, contentType, 
					nmbHeaders, nonHeaderParameters);
			if (!serverMac.equals(nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE))) {
				if (NimbusContext.instance().getPropertiesService().isDevMode()) log.debug("Valid signature is '" + serverMac + "'");
				devModeOrThrowIae("Invalid signature!");
			}
			
		} catch (Exception e) {
			log.error("Aborting API request with HTTP 400", e);
			requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
		}
	}
	
	private void devModeOrThrowIae(String msg) throws Exception {
		log.error("Bad API Request! " + msg);
		if (!NimbusContext.instance().getPropertiesService().isDevMode()) {
			log.debug("Throwing exception");
			throw new IllegalArgumentException(msg);
		}
	}
	
	private Map<String, String> toMap(MultivaluedMap<String, String> multiMap) {
		Map<String, String> map = new HashMap<>();
		for (String key : multiMap.keySet()) {
			map.put(key, multiMap.getFirst(key));
		}
		return map;
	}
}
