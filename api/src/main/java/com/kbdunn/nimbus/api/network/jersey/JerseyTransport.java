package com.kbdunn.nimbus.api.network.jersey;

import java.util.Map;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.kbdunn.nimbus.api.client.model.NimbusError;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.NimbusResponse;
import com.kbdunn.nimbus.api.network.Transport;

public class JerseyTransport implements Transport {

	public static final String JERSEY_MEDIA_TYPE = MediaType.APPLICATION_JSON;
	private static final Logger log = LoggerFactory.getLogger(JerseyTransport.class);
	
	@Override
	public <T> NimbusResponse<T> process(NimbusRequest<T> request) throws InvalidRequestException, InvalidResponseException, TransportException {
		Response response = null;
		try {
			final Client client = ClientBuilder.newClient()
					.register(JacksonJsonProvider.class)
					.register(HmacClientRequestFilter.class)
					.register(HmacClientResponseFilter.class);
					//.register(JerseyExceptionMapper.class);
			WebTarget endpoint = client.target(request.getEndpoint()).path(request.getPath());
			if (request.getMethod() == NimbusRequest.Method.GET) {
				for (Map.Entry<String, String> param : request.getParams().entrySet()) {
					endpoint = endpoint.queryParam(param.getKey(), param.getValue());
				}
			}
			final Invocation.Builder invocationBuilder = endpoint.request(JERSEY_MEDIA_TYPE);
			invocationBuilder.property(NimbusRequest.PROPERTY_NAME, request);
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
			if (statusFamily == Response.Status.Family.SUCCESSFUL) {
				responseEntity = response.readEntity(request.getReturnType());
				return new NimbusResponse<>(response.getStatus(), responseEntity);
			} else {
				responseError = response.readEntity(NimbusError.class);
				return new NimbusResponse<>(response.getStatus(), responseError);
			}
		} catch (ProcessingException e) {
			log.error("Error unmarshalling response entity", e);
			throw new InvalidResponseException("Error unmarshalling response entity", e);
		}
	}
}