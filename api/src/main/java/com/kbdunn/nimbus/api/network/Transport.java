package com.kbdunn.nimbus.api.network;

import java.io.File;

import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;

public interface Transport {
	<U, T> NimbusResponse<T> process(NimbusRequest<U, T> request) throws InvalidRequestException, InvalidResponseException, TransportException;
	<U, T> NimbusResponse<T> process(NimbusRequest<U, T> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException;
	NimbusResponse<Void> upload(NimbusRequest<File, Void> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException;
	NimbusResponse<File> download(NimbusRequest<Void, File> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException;
	void close();
}
