package com.kbdunn.nimbus.api.client;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

import com.kbdunn.nimbus.api.network.NimbusRequest;

public class DefaultOriginationIdProvider implements OriginationIdProvider {

	private static final SplittableRandom random = new SplittableRandom();
	
	private final List<OriginationIdListener> listeners = new ArrayList<>();
	
	public DefaultOriginationIdProvider() {  }
	
	public DefaultOriginationIdProvider(OriginationIdListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public <U, T> NimbusRequest<U, T> setOriginationId(NimbusRequest<U, T> request) {
		request.setOriginationId(String.valueOf(random.nextLong()));
		for (OriginationIdListener listener : listeners) {
			listener.onOriginationIdProvided(request);
		}
		return request;
	}
}