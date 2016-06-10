package com.kbdunn.nimbus.api.network.wasync;

import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Event;

import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;

public class GenericDecoder<T> implements Decoder<String, T> {

	private final Class<T> decodeClass;
	
	public GenericDecoder(Class<T> decodeClass) {
		this.decodeClass = decodeClass;
	}
	
	@Override
	public T decode(Event event, String s) {
		s = s.trim();
		if (s.length() == 0) return null; // Padding from Atmosphere
		if (!event.equals(Event.MESSAGE)) return null;
		try {
			return ObjectMapperSingleton.getMapper().readValue(s, decodeClass);
		} catch (Exception e) {
			// This decoder can't process the message
			return null;
		}
	}
}
