package com.kbdunn.nimbus.api.client.listeners;

import com.kbdunn.nimbus.common.exception.NimbusException;

public abstract class NimbusRequestListener<T> {
	
	/**
     * Called when the network call has returned and the result has been parsed.
     *
     * @param response The parsed response
     */
    public abstract void onSuccess(T response);
    
    /**
     * Called when an error occurred while processing the response.
     *
     * @param error The error that occurred
     * @param statusCode The status code of the response
     */
    public abstract void onError(NimbusException error, int statusCode);
}
