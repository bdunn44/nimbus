package com.kbdunn.nimbus.api.network;

import com.kbdunn.nimbus.api.client.listeners.AsyncTransportEventListener;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;

public interface AsyncTransport {
	<T> void connect(NimbusRequest<T> request) throws InvalidRequestException, InvalidResponseException, TransportException;
	void disconnect();
	boolean isConnected();
	void addAsyncEventListener(AsyncTransportEventListener listener);
	void removeAsyncEventListener(AsyncTransportEventListener listener);
}
