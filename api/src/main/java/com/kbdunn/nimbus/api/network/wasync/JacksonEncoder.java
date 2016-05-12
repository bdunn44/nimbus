package com.kbdunn.nimbus.api.network.wasync;

import org.atmosphere.wasync.Encoder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;

public class JacksonEncoder implements Encoder<Object, String> {
	
	@Override
	public String encode(Object s) {
		try {
			return ObjectMapperSingleton.getMapper().writeValueAsString(s);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
