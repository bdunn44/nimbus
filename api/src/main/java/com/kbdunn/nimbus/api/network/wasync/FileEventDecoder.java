package com.kbdunn.nimbus.api.network.wasync;

import java.io.IOException;

import org.atmosphere.wasync.Decoder;
import org.atmosphere.wasync.Event;

import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;

public class FileEventDecoder implements Decoder<String, FileEvent> {

	@Override
	public FileEvent decode(Event event, String s) {
		s = s.trim();
		if (s.length() == 0) return null; // Padding from Atmosphere
		if (!event.equals(Event.MESSAGE)) return null;
		try {
			return ObjectMapperSingleton.getMapper().readValue(s, FileEvent.class);
		} catch (IOException e) {
			// Bad format
			return null;
		}
	}
}
