package com.kbdunn.nimbus.api.network;

import com.kbdunn.nimbus.api.client.listeners.PushEventListener;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;

public interface PushTransport {
	<U, T> void connect(NimbusRequest<U, T> request) throws InvalidRequestException, InvalidResponseException, TransportException;
	void disconnect();
	boolean isConnected();
	void addPushEventListener(PushEventListener listener);
	void removePushEventListener(PushEventListener listener);
}
