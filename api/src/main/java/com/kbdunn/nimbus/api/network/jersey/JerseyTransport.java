package com.kbdunn.nimbus.api.network.jersey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.kbdunn.nimbus.api.client.model.NimbusError;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.NimbusResponse;
import com.kbdunn.nimbus.api.network.Transport;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.security.SSLTrustManager;

public class JerseyTransport implements Transport {

	public static final String JERSEY_MEDIA_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";
	public static final int DEFAULT_CONNECT_TIMEOUT_MS = 0; // Infinite
	public static final int DEFAULT_READ_TIMEOUT_MS = 0; // Infinite
	private static final Logger log = LoggerFactory.getLogger(JerseyTransport.class);
	
	private static final JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
	
	static {
		jsonProvider.setMapper(ObjectMapperSingleton.getMapper());
	}
	
	private Client client;
	
	public JerseyTransport() {
		initClient();
	}
	
	private void initClient() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
	    connectionManager.setMaxTotal(100);
	    connectionManager.setDefaultMaxPerRoute(20);
		this.client = ClientBuilder.newBuilder()//.newClient()
				.sslContext(SSLTrustManager.instance().getSSLContext())
				.register(HmacClientRequestFilter.class)
				.register(HmacClientResponseFilter.class)
				.register(jsonProvider)
				.register(MultiPartFeature.class)
				.property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager)
				.build()
			;
	}
	
	@Override
	public <U, T> NimbusResponse<T> process(NimbusRequest<U, T> request) throws InvalidRequestException, InvalidResponseException, TransportException {
		return process(request, DEFAULT_READ_TIMEOUT_MS);
	}
	
	@Override
	public <U, T> NimbusResponse<T> process(NimbusRequest<U, T> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (client == null) initClient();
		Response response = null;
		try {
			WebTarget endpoint = client.target(request.getEndpoint())
					.path(request.getPath())
					.property(ClientProperties.FOLLOW_REDIRECTS, true);
			if (request.getMethod() == NimbusRequest.Method.GET) {
				for (Map.Entry<String, String> param : request.getParams().entrySet()) {
					endpoint = endpoint.queryParam(param.getKey(), param.getValue());
				}
			}
			final Invocation.Builder invocationBuilder = endpoint.request(JERSEY_MEDIA_TYPE);
			invocationBuilder.property(Transport.PROPERTY_NAME, this);
			invocationBuilder.property(NimbusRequest.PROPERTY_NAME, request);
			invocationBuilder.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MS);
			invocationBuilder.property(ClientProperties.READ_TIMEOUT, readTimeout);
			final Entity<?> entity = request.getEntity() == null ? null : Entity.entity(request.getEntity(), JERSEY_MEDIA_TYPE);
			if (request.getMethod() == NimbusRequest.Method.GET) {
				response = invocationBuilder.get();
			} else if (request.getMethod() == NimbusRequest.Method.DELETE) {
				response = invocationBuilder.delete();
			} else if (request.getMethod() == NimbusRequest.Method.PUT) {
				if (request.getEntity() != null) {
					response = invocationBuilder.put(entity);
				} else {
					throw new InvalidRequestException("PUT requests cannot have a null entity!", request);
				}
			} else if (request.getMethod() == NimbusRequest.Method.POST) {
				if (request.getEntity() != null) {
					response = invocationBuilder.post(entity);
				} else {
					throw new InvalidRequestException("POST requests cannot have a null entity!", request);
				}
			} else {
				throw new InvalidRequestException("Invalid request method! (" + request.getMethod() + ")", request);
			}
		} catch (Exception e) {
			if (e instanceof TransportException) throw e;
			throw new TransportException("Error processing request", e);
		}
		try {
			Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.getStatus());
			T responseEntity = null;
			NimbusError responseError = null;
			if (statusFamily == Response.Status.Family.REDIRECTION) {
				return new NimbusResponse<>(response.getStatus(), response.getLocation());
			} else if (statusFamily == Response.Status.Family.SUCCESSFUL) {
				responseEntity = response.readEntity(request.getReturnType());
				return new NimbusResponse<>(response.getStatus(), responseEntity);
			} else {
				try {
					responseError = response.readEntity(NimbusError.class);
				} catch (Exception e) {
					responseError = new NimbusError("Request failed with HTTP " 
							+ response.getStatus() + " (" + response.getStatusInfo().getReasonPhrase() + ")");
				}
				return new NimbusResponse<>(response.getStatus(), responseError);
			}
		} catch (ProcessingException e) {
			log.error("Error unmarshalling response entity", e);
			throw new InvalidResponseException("Error unmarshalling response entity", e);
		}
	}

	@Override
	public NimbusResponse<Void> upload(NimbusRequest<File, Void> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (client == null) initClient();
		Response response = null;
		try {
			WebTarget endpoint = client.target(request.getEndpoint()).path(request.getPath());
			if (request.getMethod() == NimbusRequest.Method.GET) {
				for (Map.Entry<String, String> param : request.getParams().entrySet()) {
					endpoint = endpoint.queryParam(param.getKey(), param.getValue());
				}
			}
			MultiPart multiPart = new FormDataMultiPart();
			multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
			FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", request.getEntity(), MediaType.APPLICATION_OCTET_STREAM_TYPE);
			//fileDataBodyPart.setContentDisposition(FormDataContentDisposition.name("file").fileName(request.getEntity().getName()).build());
			multiPart.bodyPart(fileDataBodyPart);
			response = endpoint.request()//MediaType.APPLICATION_JSON_TYPE)
					//.header("Content-Type", MediaType.MULTIPART_FORM_DATA)
					.property(Transport.PROPERTY_NAME, this)
					.property(NimbusRequest.PROPERTY_NAME, request)
					.post(Entity.entity(multiPart, MediaType.MULTIPART_FORM_DATA_TYPE));//multiPart.getMediaType()));
		} catch (Exception e) {
			if (e instanceof TransportException) throw e;
			throw new TransportException("Error processing upload request", e);
		}
		try {
			Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.getStatus());
			NimbusError responseError = null;
			if (statusFamily == Response.Status.Family.SUCCESSFUL) {
				return new NimbusResponse<>(response.getStatus(), (Void) null);
			} else {
				responseError = response.readEntity(NimbusError.class);
				return new NimbusResponse<>(response.getStatus(), responseError);
			}
		} catch (ProcessingException e) {
			log.error("Error processing upload response", e);
			throw new InvalidResponseException("Error processing upload response", e);
		}
	}

	@Override
	public NimbusResponse<File> download(NimbusRequest<Void, File> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (client == null) initClient();
		Response response = null;
		try {
			WebTarget endpoint = client.target(request.getEndpoint()).path(request.getPath());
			if (request.getMethod() == NimbusRequest.Method.GET) {
				for (Map.Entry<String, String> param : request.getParams().entrySet()) {
					endpoint = endpoint.queryParam(param.getKey(), param.getValue());
				}
			}
			final Invocation.Builder invocationBuilder = endpoint.request(JERSEY_MEDIA_TYPE);
			invocationBuilder.property(Transport.PROPERTY_NAME, this);
			invocationBuilder.property(NimbusRequest.PROPERTY_NAME, request);
			invocationBuilder.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT_MS);
			invocationBuilder.property(ClientProperties.READ_TIMEOUT, readTimeout);
			invocationBuilder.header("Accept", MediaType.APPLICATION_OCTET_STREAM);
			response = invocationBuilder.get();
		} catch (Exception e) {
			if (e instanceof TransportException) throw e;
			throw new TransportException("Error processing request", e);
		}
		Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.getStatus());
		if (statusFamily != Response.Status.Family.SUCCESSFUL) {
			throw new TransportException("File download failed. " 
					+ response.getStatusInfo().getReasonPhrase() + " (HTTP " + response.getStatus() + ")");
		}
		File tmp = null;
		try (InputStream in = response.readEntity(InputStream.class);
				OutputStream out = new FileOutputStream(tmp = File.createTempFile("nimbus-", ".tmp"))) {
			byte[] buffer = new byte[2048];
			int bytesRead = 0;
			while ((bytesRead = in.read(buffer)) != -1) {
			    out.write(buffer, 0, bytesRead);
			}
			out.flush();
			return new NimbusResponse<>(response.getStatus(), tmp);
		} catch (ProcessingException | IOException e) {
			log.error("Error processing file download response stream", e);
			throw new InvalidResponseException("Error processing file download response stream", e);
		}
	}

	@Override
	public void close() {
		client.close();
		client = null;
	}
}