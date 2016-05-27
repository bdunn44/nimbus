package com.kbdunn.nimbus.api.network;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.GenericType;

import com.kbdunn.nimbus.api.client.listeners.NimbusRequestListener;
import com.kbdunn.nimbus.api.client.model.NimbusApiCredentials;

// Generics represent <Request Entity, Response Entity>
public class NimbusRequest<U, T> {

    public interface Method {
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        
        public static String getVerbString(int method) {
        	if (method == 0) return "GET";
        	if (method == 1) return "POST";
        	if (method == 2) return "PUT";
        	if (method == 3) return "DELETE";
        	return "";
        }
    }
    
	public static final String PROPERTY_NAME = NimbusRequest.class.getCanonicalName();

    private final NimbusApiCredentials creds;
    private final int method;
    private final String endpoint;
    private final String path;
    private final U entity;
    private final Map<String, String> params;
    private final NimbusRequestListener<T> listener;
    private final GenericType<T> returnType;
    private String originationId;
    
	public NimbusRequest(NimbusApiCredentials creds, int method, String endpoint, String path, GenericType<T> returnType) {
		this(creds, method, endpoint, path, (U) null, returnType);
	}
    
    public NimbusRequest(NimbusApiCredentials creds, int method, String endpoint, String path, NimbusRequestListener<T> listener, GenericType<T> returnType) {
        this(creds, method, endpoint, path, (U) null, listener, returnType);
    }
    
    public NimbusRequest(NimbusApiCredentials connection, int method, String endpoint, String path, Map<String, String> params, NimbusRequestListener<T> listener, GenericType<T> returnType) {
        this(connection, method, endpoint, path, params, null, listener, returnType);
    }
    
    public NimbusRequest(NimbusApiCredentials connection, int method, String endpoint, String path, U entity, GenericType<T> returnType) {
        this(connection, method, endpoint, path, null, entity, null, returnType);
    }
    
    public NimbusRequest(NimbusApiCredentials connection, int method, String endpoint, String path, U entity, NimbusRequestListener<T> listener, GenericType<T> returnType) {
        this(connection, method, endpoint, path, null, entity, listener, returnType);
    }

    private NimbusRequest(NimbusApiCredentials connection, int method, String endpoint, String path, Map<String, String> params, U entity, NimbusRequestListener<T> listener, GenericType<T> returnType) {
        this.creds = connection;
        this.method = method;
        this.endpoint = endpoint;
        this.path = path;
        this.params = params;
        this.entity = entity;
        this.listener = listener;
        this.returnType = returnType;
    }

	public NimbusApiCredentials getCredentials() {
		return creds;
	}

	public int getMethod() {
		return method;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public String getPath() {
		return path;
	}

	public U getEntity() {
		return entity;
	}

	public Map<String, String> getParams() {
		if (params == null) return Collections.emptyMap();
		return params;
	}

	public NimbusRequestListener<T> getListener() {
		return listener;
	}
	
	public GenericType<T> getReturnType() {
		return returnType;
	}
	
	public String getOriginationId() {
		return originationId;
	}
	
	public void setOriginationId(String originationId) {
		this.originationId = originationId;
	}
}
