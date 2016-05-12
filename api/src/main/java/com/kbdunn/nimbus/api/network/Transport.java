package com.kbdunn.nimbus.api.network;

import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;

public interface Transport {
	<T> NimbusResponse<T> process(NimbusRequest<T> request) throws InvalidRequestException, InvalidResponseException, TransportException;
}
