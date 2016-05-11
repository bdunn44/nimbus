package com.kbdunn.nimbus.api.network.atmosphere;

import java.io.IOException;

import javax.ws.rs.core.MediaType;

import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.impl.AtmosphereClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusAsyncTransport;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.NimbusResponse;

public class AtmosphereTransport implements NimbusAsyncTransport {

	private static final Logger log = LoggerFactory.getLogger(AtmosphereTransport.class);
	
	@Override
	public <T> NimbusResponse<T> process(NimbusRequest<T> request) throws InvalidRequestException, InvalidResponseException, TransportException {
		final Client client = ClientFactory.getDefault().newClient(AtmosphereClient.class);
		final RequestBuilder builder = client.newRequestBuilder()
				.method(Request.METHOD.GET)
				.header("Content-Type", "application/json")
				.uri(request.getEndpoint() + request.getPath())
				//.trackMessageLength(true)
				.transport(Request.TRANSPORT.WEBSOCKET);
				//.transport(Request.TRANSPORT.SSE)
				//.transport(Request.TRANSPORT.STREAMING)
				//.transport(Request.TRANSPORT.LONG_POLLING);
		
		final Socket socket = client.create();
		try {
			socket.on("message", new Function<String>() {
				@Override
				public void on(String t) {
					// TODO Auto-generated method stub
					
				}
			}).on(new Function<Throwable>() {
				@Override
				public void on(Throwable t) {
					log.error("Unexpected error encountered", t);
				}
			}).on(Event.CLOSE.name(), new Function<String>() {

				@Override
				public void on(String t) {
					log.debug("Socket closed");
					socket.close();
				}
			}).open(builder.build());
			
			socket.fire("TEST!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
