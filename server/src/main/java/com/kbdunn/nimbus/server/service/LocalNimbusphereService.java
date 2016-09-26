package com.kbdunn.nimbus.server.service;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.glassfish.jersey.client.ClientProperties;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.security.SSLTrustManager;
import com.kbdunn.nimbus.common.model.nimbusphere.HeartbeatRequest;
import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.common.model.nimbusphere.VerifyRequest;
import com.kbdunn.nimbus.common.model.nimbusphere.VerifyResponse;
import com.kbdunn.nimbus.common.server.NimbusphereService;
import com.kbdunn.nimbus.common.util.TrackedScheduledThreadPoolExecutor;
import com.kbdunn.nimbus.server.NimbusContext;
import com.kbdunn.nimbus.server.dao.NimbusSystemDAO;

public class LocalNimbusphereService implements NimbusphereService {
	
	private static final Logger log = LogManager.getLogger(LocalNimbusphereService.class);

	public static final String JERSEY_MEDIA_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";
	public static final int DEFAULT_CONNECT_TIMEOUT_MS = 0; // Infinite
	public static final int DEFAULT_READ_TIMEOUT_MS = 0; // Infinite
	
	private static final JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
	
	static {
		jsonProvider.setMapper(ObjectMapperSingleton.getMapper());
	}
	
	private LocalPropertiesService propertiesService;
	private Client client;
	
	private TrackedScheduledThreadPoolExecutor executor;
	private ScheduledFuture<?> heartbeatFuture;
	
	public LocalNimbusphereService() {
		this.client = ClientBuilder.newBuilder()
			.sslContext(SSLTrustManager.instance().getSSLContext())
			.register(jsonProvider)
			.build()
		;
	}
	
	public void initialize(NimbusContext context) {
		this.propertiesService = context.getPropertiesService();
	}
	
	@Override
	public VerifyResponse verify(String token) throws TransportException, ProcessingException {
		final Entity<VerifyRequest> entity = Entity.entity(new VerifyRequest(propertiesService.getNimbusVersion()), JERSEY_MEDIA_TYPE);
		Response response = null;
		try {
			WebTarget endpoint = client.target("https://nimbusphere.org")
					.path("/api/v1/verify/cloud/" + token)
					.property(ClientProperties.FOLLOW_REDIRECTS, true);
			response = endpoint.request(JERSEY_MEDIA_TYPE).put(entity);
		} catch (Exception e) {
			if (e instanceof TransportException) throw e;
			throw new TransportException("Error processing verification request", e);
		}
		Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.getStatus());
		if (statusFamily == Response.Status.Family.SUCCESSFUL) {
			final VerifyResponse verified = response.readEntity(VerifyResponse.class);
			NimbusSystemDAO.updateNimbusphereStatus(new NimbusphereStatus(null, verified.getToken(), null, null, null, null));
			this.startHeartbeat();
			return verified;
		} else {
			throw new TransportException("Verification request failed with HTTP " + response.getStatus());
		}
	}
	
	public NimbusphereStatus doHeartbeat() throws TransportException, ProcessingException {
		final String token = propertiesService.getNimbusphereStatus().getToken();
		if (token == null) {
			if (this.heartbeatFuture != null && !this.heartbeatFuture.isDone()) {
				this.heartbeatFuture.cancel(true);
			}
			return null;
		}
		log.debug("Sending heartbeat: " + new HeartbeatRequest(token, propertiesService.getNimbusVersion()));
		final Entity<HeartbeatRequest> entity = Entity.entity(new HeartbeatRequest(token, propertiesService.getNimbusVersion()), JERSEY_MEDIA_TYPE);
		Response response = null;
		try {
			WebTarget endpoint = client.target("https://nimbusphere.org")
					.path("/api/v1/ddns/heartbeat")
					.property(ClientProperties.FOLLOW_REDIRECTS, true);
			response = endpoint.request(JERSEY_MEDIA_TYPE).post(entity);
		} catch (Exception e) {
			if (e instanceof TransportException) throw e;
			throw new TransportException("Error processing heartbeat request", e);
		}
		Response.Status.Family statusFamily = Response.Status.Family.familyOf(response.getStatus());
		if (statusFamily == Response.Status.Family.SUCCESSFUL) {
			return response.readEntity(NimbusphereStatus.class);
		} else {
			throw new TransportException("Heartbeat request failed with HTTP " + response.getStatus());
		}
	}
	
	public void startHeartbeat() {
		if (NimbusSystemDAO.getNimbusphereStatus().getToken() == null) return;
		if (this.executor == null) {
			this.executor = new TrackedScheduledThreadPoolExecutor(1);
		}
		if (this.heartbeatFuture != null && !this.heartbeatFuture.isDone()) return;
		
		this.heartbeatFuture = this.executor.scheduleAtFixedRate(() -> {
			try {
				final NimbusphereStatus response = this.doHeartbeat();
				log.debug("Heartbeat response: " + response);
				if (response.isAccepted()) {
					NimbusSystemDAO.updateNimbusphereStatus(response);
				} else {
					log.warn("Heartbeat request was not accepted");
					// Token is now invalid. Set status to deleted.
					NimbusphereStatus s = NimbusSystemDAO.getNimbusphereStatus();
					NimbusSystemDAO.updateNimbusphereStatus(new NimbusphereStatus(null, s.getToken(), null, null, true, true));
					// Cancel heartbeat
					if (this.heartbeatFuture != null && !this.heartbeatFuture.isDone()) {
						this.heartbeatFuture.cancel(true);
						log.debug("Heartbeat cancelled");
					}
				}
			} catch (Exception e) {
				log.error("Heartbeat failed", e);
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
}
