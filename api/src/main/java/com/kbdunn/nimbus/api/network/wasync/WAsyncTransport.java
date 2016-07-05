package com.kbdunn.nimbus.api.network.wasync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Socket.STATUS;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.atmosphere.wasync.impl.AtmosphereRequest.AtmosphereRequestBuilder;
import org.atmosphere.wasync.impl.DefaultOptions;
import org.atmosphere.wasync.impl.DefaultOptionsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kbdunn.nimbus.api.client.listeners.PushEventListener;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.SyncRootChangeEvent;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusHttpHeaders;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.PushTransport;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.api.network.util.HmacUtil;

public class WAsyncTransport implements PushTransport {

	private static final Logger log = LoggerFactory.getLogger(WAsyncTransport.class);

	private final Client<DefaultOptions, DefaultOptionsBuilder, AtmosphereRequestBuilder> client;
	private final List<PushEventListener> listeners;
	private Socket socket;
	private boolean isConnected = false;
	
	private String transport;
	
	public WAsyncTransport() {
		this.client = ClientFactory.getDefault().newClient(AtmosphereClient.class);
		this.listeners = new ArrayList<>();
	}
	
	@Override
	public <U, T> void connect(NimbusRequest<U, T> request) throws InvalidRequestException, InvalidResponseException, TransportException {
		// HMAC signature
		NimbusHttpHeaders nHeaders = new NimbusHttpHeaders();
		String verb = "GET";
		String resource;
		String requestor = request.getCredentials().getUsername();
		String ts;
		try {
			resource = new URI(request.getEndpoint() + request.getPath()).getPath();
			ts = ObjectMapperSingleton.getMapper().writeValueAsString(new Date()).replace("\"", ""); // unquote;
		} catch (URISyntaxException e) {
			throw new InvalidRequestException("Invalid target URI " + request.getEndpoint() + request.getPath(), request);
		} catch (JsonProcessingException e) {
			throw new TransportException("Error initiating async request", e);
		}
		nHeaders.put(NimbusHttpHeaders.Key.REQUESTOR, requestor);
		nHeaders.put(NimbusHttpHeaders.Key.TIMESTAMP, ts);
		try {
			nHeaders.put(NimbusHttpHeaders.Key.SIGNATURE, HmacUtil.hmacDigestRequest(
					request.getCredentials().getApiToken(), 
					verb, 
					resource, 
					null, 
					null, 
					nHeaders, 
					null
				));
		} catch (Exception e) {
			throw new TransportException("Error signing async request", e);
		}
		
		final RequestBuilder<AtmosphereRequestBuilder> builder = client.newRequestBuilder()
				.method(Request.METHOD.GET)
				.header("Content-Type", "application/json")
				.uri(request.getEndpoint() + request.getPath())
				.header(NimbusHttpHeaders.Key.REQUESTOR, nHeaders.get(NimbusHttpHeaders.Key.REQUESTOR))
				.header(NimbusHttpHeaders.Key.TIMESTAMP, nHeaders.get(NimbusHttpHeaders.Key.TIMESTAMP))
				.header(NimbusHttpHeaders.Key.SIGNATURE, nHeaders.get(NimbusHttpHeaders.Key.SIGNATURE))
				.trackMessageLength(true)
				.encoder(new JacksonEncoder())
				.decoder(new GenericDecoder<FileEvent>(FileEvent.class))
				.decoder(new GenericDecoder<SyncRootChangeEvent>(SyncRootChangeEvent.class))
				.transport(Request.TRANSPORT.WEBSOCKET)
				.transport(Request.TRANSPORT.SSE)
				.transport(Request.TRANSPORT.STREAMING)
				.transport(Request.TRANSPORT.LONG_POLLING);
		
		socket = client.create();
		
		try {
			socket.on(new Function<Throwable>() {
				@Override
				public void on(Throwable t) {
					log.error("Unexpected error encountered during async communication", t);
					checkConnection();
					for (PushEventListener listener : listeners) {
						listener.onError(WAsyncTransport.this, t);
					}
				}
			}).on(Event.CLOSE.name(), new Function<String>() {
				@Override
				public void on(String t) {
					log.info("Socket closed. Current status is {}", socket.status());
					if (socket.status() != Socket.STATUS.CLOSE) {
						socket.close();
					}
					setConnected(false);
				}
			}).on(Event.OPEN.name(), new Function<String>() {
				@Override
				public void on(String t) {
					log.info("Socket opened. Current status is {}", socket.status());
					setConnected(true);
				}
			}).on(Event.REOPENED.name(), new Function<String>() {
				@Override
				public void on(String t) {
					log.info("Socket reopened. Current status is {}", socket.status());
					setConnected(true);
				}
			}).on(Event.ERROR.name(), new Function<String>() {
				@Override
				public void on(String t) {
					log.info("Socket error: {}", t);
					for (PushEventListener listener : listeners) {
						listener.onError(WAsyncTransport.this, new TransportException(t));
					}
					checkConnection();
				}
			}).on(Event.TRANSPORT.name(), new Function<Request.TRANSPORT>() {
				@Override
				public void on(Request.TRANSPORT t) {
					if (transport == null || !t.toString().equals(transport)) {
						transport = t.toString();
						log.info("Transport is: {}", t);
					}
				}
			})/*.on(new Function<Map<String, ArrayList<String>>>() {
			
				@Override
				public void on(Map<String, ArrayList<String>> t) {
					log.info("HEADERS event called.");
					
					for (Map.Entry<String, ArrayList<String>> e : t.entrySet()) {
						log.info("  " + e.getKey() + "=" + e.getValue().get(0));
					}
				}
			})*/.on(Event.MESSAGE.toString(), new Function<String>() {
				@Override
				public void on(String event) {
					// This is for debugging purposes only
					log.debug("Recieved string message: " + event);
				}
			}).on(Event.MESSAGE.toString(), new Function<FileEvent>() {
				@Override
				public void on(FileEvent event) {
					for (PushEventListener listener : listeners) {
						listener.onFileEvent(WAsyncTransport.this, event);
					}
				}
			}).on(Event.MESSAGE.toString(), new Function<SyncRootChangeEvent>() {
				@Override
				public void on(SyncRootChangeEvent event) {
					for (PushEventListener listener : listeners) {
						listener.onSyncRootChangeEvent(WAsyncTransport.this, event);
					}
				}
			}).open(builder.build());
			
		} catch (IOException e) {
			throw new TransportException("Async communication error", e);
		}
	}

	@Override
	public boolean isConnected() {
		return isConnected;
	}
	
	@Override
	public void disconnect() {
		if (socket != null) socket.close();
		setConnected(false);
	}
	
	@Override
	public void addPushEventListener(PushEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removePushEventListener(PushEventListener listener) {
		listeners.remove(listener);
	}
	
	private void checkConnection() {
		if (socket.status() == STATUS.CLOSE) {
			setConnected(false);
		}
	}
	
	private void setConnected(boolean isConnected) {
		if (this.isConnected != isConnected) {
			this.isConnected = isConnected;
			if (isConnected) {
				for (PushEventListener listener : listeners) {
					listener.onConnect(this);
				}
			} else {
				for (PushEventListener listener : listeners) {
					listener.onClose(this);
				}
			}
		}
	}
}
