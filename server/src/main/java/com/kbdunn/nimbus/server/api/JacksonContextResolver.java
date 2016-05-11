package com.kbdunn.nimbus.server.api;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.api.network.jersey.ObjectMapperSingleton;

@Provider
//@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {
	
    private JacksonContextResolver() {  }
    
    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return ObjectMapperSingleton.getMapper();
    }
}