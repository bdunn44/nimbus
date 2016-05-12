package com.kbdunn.nimbus.api.client.listeners;

import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.network.AsyncTransport;

public interface AsyncTransportEventListener {
	void onClose(AsyncTransport transport);
	void onConnect(AsyncTransport transport);
	void onFileEvent(AsyncTransport transport, FileEvent event);
	void onError(AsyncTransport transport, Throwable t);
}
