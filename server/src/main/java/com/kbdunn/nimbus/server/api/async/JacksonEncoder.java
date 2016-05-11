package com.kbdunn.nimbus.server.api.async;

import org.atmosphere.config.managed.Encoder;

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
