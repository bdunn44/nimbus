package com.kbdunn.nimbus.api.client;

import com.kbdunn.nimbus.api.network.NimbusRequest;

public interface OriginationIdProvider {
	<U, T> NimbusRequest<U, T> setOriginationId(NimbusRequest<U, T> request);
	
	public interface OriginationIdListener {
		void onOriginationIdProvided(NimbusRequest<?, ?> request);
	}
}
