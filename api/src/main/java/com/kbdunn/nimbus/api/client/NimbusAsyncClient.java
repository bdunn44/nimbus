package com.kbdunn.nimbus.api.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.ws.rs.core.GenericType;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kbdunn.nimbus.api.client.listeners.AsyncTransportEventListener;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.NimbusApiCredentials;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.AsyncTransport;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.wasync.AtmosphereTransport;
import com.kbdunn.nimbus.common.exception.NimbusException;

public class NimbusAsyncClient implements AsyncTransportEventListener {

	public static final long DEFAULT_CONNECT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);
	public static final String API_PATH = "/async";
	
	private static final Logger log = LoggerFactory.getLogger(NimbusAsyncClient.class);
	
	private final URL url;
	private final NimbusApiCredentials credentials;
	private final AsyncTransport transport;
	private final List<AsyncTransportEventListener> asyncEventListeners = new ArrayList<>();
	private CountDownLatch connectLatch;
	
	public NimbusAsyncClient(String url, NimbusApiCredentials credentials) throws MalformedURLException {
		this.url = new URL(url);
		this.credentials = credentials;
		transport = new AtmosphereTransport();
		transport.addAsyncEventListener(this);
	}
	
	public static void main(String[] args) {
		LogManager.getRootLogger().removeAllAppenders(); // Clear log4j configuration
		java.util.logging.LogManager.getLogManager().reset(); // Clear JUL configuration
		java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST); // Set JUL to FINEST (slf4j then filters messages)
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory()); 
		Properties log4jprops = new Properties();
		InputStream is = null;
		try {
			is = NimbusAsyncClient.class.getResourceAsStream("/log4j.properties");
			log4jprops.load(is);
		} catch (IOException e) {
			//e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		PropertyConfigurator.configure(log4jprops);
		final Logger log = LoggerFactory.getLogger(NimbusAsyncClient.class);
		log.info("Testing Atmosphere client connection....");
		try {
			NimbusAsyncClient client = new NimbusAsyncClient("http://localhost:8080", 
					new NimbusApiCredentials(
							"d6c48e3208bfed94c100b0d5aa7232793f60c2630816d15fc35895a65c18992b", 
							"769fbec5b880e85f5519e9c05f2a76aa630dbb95cf536ef125faae3b401464f9")
					);
			log.info("Connected? " + client.connect());
			new Thread(() -> {
				try {
					Thread.sleep(3000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				client.disconnect();
			}).start();
		} catch (NimbusException e) {
			//log.error("", e);
		} catch (MalformedURLException e) {
			//log.error("", e);
		}
	}
	
	public URL getUrl() {
		return url;
	}
	
	public String getApiEndpoint() {
		if (url == null) return null;
		return url.getProtocol() + "://" + url.getHost() + (url.getPort() == -1 ? "" : ":" + url.getPort()) + API_PATH;
	}
	
	public boolean isConnected() {
		return transport.isConnected();
	}
	
	public boolean connect() throws TransportException {
		return connect(DEFAULT_CONNECT_TIMEOUT_MS);
	}
	
	public boolean connect(long timeout) throws TransportException {
		log.info("Connecting to async event bus.");
		connectLatch = new CountDownLatch(1);
		transport.connect(new NimbusRequest<>(
				credentials, 
				NimbusRequest.Method.GET, 
				getApiEndpoint(), 
				"/eventbus",
				new GenericType<String>(){})
			);
		try {
			if (connectLatch != null && !connectLatch.await(timeout, TimeUnit.MILLISECONDS)) {
				log.error("Aborting async connection attempt after " + timeout + "ms." );
			}
		} catch (InterruptedException e) {
			log.error("Connection attempt interrupted", e);
		}
		return transport.isConnected();
	}
	
	public void disconnect() {
		log.info("Disconnecting from async event bus.");
		transport.disconnect();
	}

	@Override
	public void onClose(AsyncTransport transport) {
		for (AsyncTransportEventListener listener : asyncEventListeners) {
			listener.onClose(transport);
		}
	}

	@Override
	public void onConnect(AsyncTransport transport) {
		if (connectLatch != null) {
			connectLatch.countDown();
			connectLatch = null;
		}
		for (AsyncTransportEventListener listener : asyncEventListeners) {
			listener.onConnect(transport);
		}
	}
	
	@Override
	public void onFileEvent(AsyncTransport transport, FileEvent event) {
		for (AsyncTransportEventListener listener : asyncEventListeners) {
			listener.onFileEvent(transport, event);
		}
	}
	
	@Override
	public void onError(AsyncTransport transport, Throwable t) {
		for (AsyncTransportEventListener listener : asyncEventListeners) {
			listener.onError(transport, t);
		}
	}
}
