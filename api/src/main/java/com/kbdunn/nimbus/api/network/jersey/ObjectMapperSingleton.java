package com.kbdunn.nimbus.api.network.jersey;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbdunn.nimbus.common.util.DateUtil;

public class ObjectMapperSingleton {

	private static ObjectMapper objectMapper;
	
	static {
		objectMapper = new ObjectMapper();
		configureMapper();
	}
	
    private ObjectMapperSingleton() {  }
    
    public static ObjectMapper getMapper() {
    	return objectMapper;
    }
    
    private static void configureMapper() {
    	// Configure mapper
        objectMapper
        	.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        	.setDateFormat(DateUtil.getDateFormat());
        
        // Pretty print if DEV mode
        /*if (new LocalPropertiesService().isDevMode()) {
	        objectMapper
		        .configure(SerializationFeature.INDENT_OUTPUT, true);
        }*/
    }
}
