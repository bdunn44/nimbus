package com.kbdunn.nimbus.api.network.jersey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.message.internal.ReaderWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.model.NimbusApiCredentials;
import com.kbdunn.nimbus.api.client.model.NimbusError;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.util.HmacUtil;
import com.kbdunn.nimbus.api.network.util.UriUtil;
import com.kbdunn.nimbus.common.util.DateUtil;

@Provider
@Priority(1) // Execute first
public class HmacClientResponseFilter implements ClientResponseFilter {

	private static final Logger log = LoggerFactory.getLogger(HmacClientResponseFilter.class.getName());
	
	public static final String REQUEST_API_TOKEN = "apiToken";
	
	@Override
	public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
		Map<String, String> headers = UriUtil.toSingleValuedStringMap(responseContext.getHeaders());
		NimbusHttpHeaders nmbHeaders = NimbusHttpHeaders.fromMap(headers).coalesce(headers);
		
		String status = String.valueOf(responseContext.getStatus());
		String contentType = responseContext.getMediaType() == null ? null : responseContext.getHeaderString("Content-Type");
		String content = null;
		
		if (responseContext.getEntityStream() != null
				&& (contentType == null || (!contentType.contains(MediaType.APPLICATION_OCTET_STREAM) 
						&& !contentType.contains(MediaType.MULTIPART_FORM_DATA)))) {
			// Read entity, reset the input stream
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = responseContext.getEntityStream();
			try {
				if (in.available() > 0) {
					ReaderWriter.writeTo(in, out);
					byte[] entity = out.toByteArray();
					content = new String(entity);
					responseContext.setEntityStream(new ByteArrayInputStream(entity));
				}
			} catch (IOException e) {
				throw new IllegalStateException("Unable to read response content!", e);
			}
		} 
		
		/*log.debug("Nimbus API Response:");
		log.debug("    status: " + status);
		log.debug("    requestor: " + nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR));
		log.debug("    timestamp: " + nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP));
		log.debug("    signature: " + nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE));
		log.debug("    content: " + content);
		log.debug("    content type: " + contentType);*/
		
		try {
			// Check required parameters are present
			if (status == null
					|| nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR) == null 
					|| nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP) == null 
					|| nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE) == null) {
				throw new IllegalArgumentException("Invalid response - verb, requestor, timestamp, or signature not present.");
			}
			
			// Check response requestor matches request requestor
			final NimbusApiCredentials creds = ((NimbusRequest<?, ?>)requestContext.getProperty(NimbusRequest.PROPERTY_NAME)).getCredentials();
			if (!nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR).equals(creds.getApiToken()))
				throw new IllegalArgumentException("Invalid API token in response (" + nmbHeaders.get(NimbusHttpHeaders.Key.REQUESTOR) + ")!");
			
			// Check timestamp isn't old
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateUtil.JAVA8_DATE_FORMAT);
			LocalDateTime datetime = LocalDateTime.parse(nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP), formatter);
			if (datetime.compareTo(LocalDateTime.now().minusMinutes(5)) == -1) {
				throw new IllegalArgumentException("Stale response!  (" + nmbHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP) + ")");
			}
			
			// Check mac hash
			String clientMac = HmacUtil.hmacDigestResponse(
					creds.getHmacKey(), 
					status, 
					content, 
					contentType, 
					nmbHeaders);
			if (!clientMac.equals(nmbHeaders.get(NimbusHttpHeaders.Key.SIGNATURE))) {
				//if (NimbusContext.instance().getPropertiesService().isDevMode()) 
					log.debug("Valid signature is '" + clientMac + "'");
					throw new IllegalArgumentException("Invalid HMAC signature!");
			}
			
		} catch (Exception e) {
			final NimbusError err = new NimbusError(e.getMessage());
			String errjson = ObjectMapperSingleton.getMapper().writeValueAsString(err);
			responseContext.setEntityStream(new ByteArrayInputStream(errjson.getBytes()));
			responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			log.error("Error encountered during HMAC validation", e);
			/*throw new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity(new NimbusError(e.getMessage()))
					.build()
				);*/
		}
	}
}