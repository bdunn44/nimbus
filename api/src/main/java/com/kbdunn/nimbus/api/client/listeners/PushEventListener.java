package com.kbdunn.nimbus.api.client.listeners;

import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.network.PushTransport;

public interface PushEventListener {
	void onClose(PushTransport transport);
	void onConnect(PushTransport transport);
	void onFileEvent(PushTransport transport, FileEvent event);
	void onError(PushTransport transport, Throwable t);
}
