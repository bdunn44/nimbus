package com.kbdunn.nimbus.server.api.async;

import java.io.IOException;

import org.atmosphere.config.managed.Decoder;

import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;

public class FileEventDecoder implements Decoder<String, FileEvent> {

	@Override
	public FileEvent decode(String s) {
		try {
			return ObjectMapperSingleton.getMapper().readValue(s, FileEvent.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
