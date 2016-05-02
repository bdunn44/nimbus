package com.kbdunn.nimbus.server.api;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kbdunn.nimbus.common.util.DateUtil;
import com.kbdunn.nimbus.server.service.LocalPropertiesService;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JacksonContextResolver implements ContextResolver<ObjectMapper> {
	
	private ObjectMapper objectMapper;
	
    public JacksonContextResolver() throws Exception {
        this.objectMapper = new ObjectMapper();
        
        // Configure mapper
        this.objectMapper
        	.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        	.setDateFormat(DateUtil.getDateFormat());
        
        // Pretty print if DEV mode
        if (new LocalPropertiesService().isDevMode()) {
	        this.objectMapper
		        .configure(SerializationFeature.INDENT_OUTPUT, true);
        }
    }
    
    @Override
    public ObjectMapper getContext(Class<?> objectType) {
        return objectMapper;
    }
}