package com.kbdunn.nimbus.api.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
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

import com.kbdunn.nimbus.api.client.OriginationIdProvider.OriginationIdListener;
import com.kbdunn.nimbus.api.client.listeners.PushEventListener;
import com.kbdunn.nimbus.api.client.model.AuthenticateResponse;
import com.kbdunn.nimbus.api.client.model.FileEvent;
import com.kbdunn.nimbus.api.client.model.NimbusApiCredentials;
import com.kbdunn.nimbus.api.client.model.SyncFile;
import com.kbdunn.nimbus.api.client.model.SyncRootChangeEvent;
import com.kbdunn.nimbus.api.exception.InvalidRequestException;
import com.kbdunn.nimbus.api.exception.InvalidResponseException;
import com.kbdunn.nimbus.api.exception.TransportException;
import com.kbdunn.nimbus.api.network.NimbusRequest;
import com.kbdunn.nimbus.api.network.NimbusResponse;
import com.kbdunn.nimbus.api.network.PushTransport;
import com.kbdunn.nimbus.api.network.Transport;
import com.kbdunn.nimbus.api.network.jersey.JerseyTransport;
import com.kbdunn.nimbus.api.network.security.SSLTrustManager;
import com.kbdunn.nimbus.api.network.wasync.WAsyncTransport;

public class NimbusClient implements PushEventListener, OriginationIdListener {

	public interface Type {
		int HTTP = 0;
	}
	
	public static final long DEFAULT_CONNECT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);
	public static final String API_PATH = "/request";
	public static final String ASYNC_ENDPOINT = "/async";
	
	private static final Logger log = LoggerFactory.getLogger(NimbusClient.class);
	
	private URL url;
	private final NimbusApiCredentials credentials;
	private boolean isAuthenticated = false;
	private Transport transport;
	private PushTransport pushTransport;
	private final List<PushEventListener> pushEventListeners = new ArrayList<>();
	private OriginationIdProvider originationIdProvider;
	private final List<OriginationIdListener> originationIdListeners = new ArrayList<>();
	private OriginationIdFilter originationIdFilter;
	
	public NimbusClient(String url, NimbusApiCredentials credentials, int type) throws MalformedURLException {
		if (!url.startsWith("http")) {
			url = "http://" + url;
		}
		setUrl(new URL(url));
		this.credentials = credentials;
		pushTransport = new WAsyncTransport();
		pushTransport.addPushEventListener(this);
		if (type == Type.HTTP) transport = new JerseyTransport();
		else throw new IllegalArgumentException("Invalid client type");
		originationIdProvider = new DefaultOriginationIdProvider(this);
	}
	
	public static void main(String[] args) {
		LogManager.getRootLogger().removeAllAppenders(); // Clear log4j configuration
		java.util.logging.LogManager.getLogManager().reset(); // Clear JUL configuration
		java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST); // Set JUL to FINEST (slf4j then filters messages)
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory()); 
		Properties log4jprops = new Properties();
		InputStream is = null;
		try {
			is = NimbusClient.class.getResourceAsStream("/log4j.properties");
			log4jprops.load(is);
		} catch (IOException e) {
			//e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}
		PropertyConfigurator.configure(log4jprops);
		final Logger log = LoggerFactory.getLogger(NimbusClient.class);
		log.info("Testing client connection....");
		FileSyncManager syncManager = null;
		NimbusClient client = null;
		try {
			client = new NimbusClient("http://localhost:8080", 
					new NimbusApiCredentials(
							"d6c48e3208bfed94c100b0d5aa7232793f60c2630816d15fc35895a65c18992b", 
							"769fbec5b880e85f5519e9c05f2a76aa630dbb95cf536ef125faae3b401464f9")
					, Type.HTTP);
			client.authenticate();
			log.info("Authenticated? " + client.isAuthenticated());
			Thread.sleep(3000);
			syncManager = new FileSyncManager(client);
			log.debug("Getting Sync File List");
			List<SyncFile> list = syncManager.getSyncFileList();
			log.debug("Response returned {} files", list.size());
			for (SyncFile file : list) log.debug("\t{}", file);
			Thread.sleep(3000);
			log.debug("Copying file");
			syncManager.copy(new SyncFile("/Videos/Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false), 
					new SyncFile("/Videos/another-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false),
					false);
			Thread.sleep(3000);
			log.debug("Moving file");
			syncManager.move(new SyncFile("/Videos/another-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false), 
					new SyncFile("/Videos/moved-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false),
					false);
			Thread.sleep(3000);
			log.debug("Creating Directory");
			syncManager.createDirectory(new SyncFile("/Videos/newApiDirectory", "", true));
			Thread.sleep(3000);
			log.debug("Downloading file");
			File dl = syncManager.download(new SyncFile("/Videos/Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false));
			log.debug("File downloaded to {}", dl);
			Thread.sleep(3000);
			//File dl = new File("C:\\Users\\kdunn\\AppData\\Local\\Temp\\nimbus-1107206217771797958.tmp");
			log.debug("Uploading file");
			syncManager.upload(new SyncFile("/Videos/newApiDirectory/uploaded-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false), dl);
			Thread.sleep(3000);
			log.debug("Getting sync file /Videos/newApiDirectory/uploaded-Wildlife.wmv");
			log.debug("Server response is {}", syncManager.getSyncFile("/Videos/newApiDirectory/uploaded-Wildlife.wmv"));
			
			Thread.sleep(6000);
			log.debug("Done");
		} catch (Exception e) {
			log.error("Error encountered during test", e);
		} finally {
			if (syncManager != null) {
				log.debug("Deleting test files");
				try { syncManager.delete(new SyncFile("/Videos/another-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false)); } catch (TransportException e) {  }
				try { syncManager.delete(new SyncFile("/Videos/moved-Wildlife.wmv", "D8C2EAFD90C266E19AB9DCACC479F8AF", false)); } catch (TransportException e) {  }
				try { syncManager.delete(new SyncFile("/Videos/newApiDirectory", "", true)); } catch (TransportException e) {  }
				if (client != null) client.disconnect();
			}
		}
	}
	
	public URL getUrl() {
		return url;
	}
	
	public String getUrlString() {
		return url.getProtocol() + "://" + url.getAuthority();
	}
	
	private void setUrl(URL url) {
		this.url = url;
		if (url.getHost().equalsIgnoreCase("localhost"))
			SSLTrustManager.instance().disableSSLVerification();
	}
	
	public String getApiEndpoint() {
		if (url == null) return null;
		return getUrlString() + API_PATH;
	}
	
	public String getAsyncEndpoint() {
		if (url == null) return null;
		return getUrlString() + ASYNC_ENDPOINT;
	}
	
	protected NimbusApiCredentials getCredentials() {
		return credentials;
	}
	
	public boolean isAuthenticated() {
		return isAuthenticated;
	}
	
	public boolean authenticate() {
		return authenticate(DEFAULT_CONNECT_TIMEOUT_MS);
	}
	
	public boolean authenticate(long timeout) {
		boolean success = false;
		if (transport == null) transport = new JerseyTransport();
		final CountDownLatch connectLatch = new CountDownLatch(1);
		final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
		final ScheduledFuture<?> reconnectFuture = reconnectExecutor.scheduleAtFixedRate(() -> {
			try {
				log.info("Authenticating with the Nimbus API...");

				final NimbusRequest<Void, AuthenticateResponse> request = getAuthenticateRequest();
				NimbusResponse<AuthenticateResponse> response = process(request);
				if (response.redirected()) {
					log.info("Server sent HTTP " + response.getStatus() + " redirect to " + response.getRedirectURI());
					// Process again with new URL
					URI redirect = response.getRedirectURI();
					setUrl(new URL(redirect.getScheme() + "://" + redirect.getAuthority()));
					request.setEndpoint(getApiEndpoint());
					response = process(request);
				}
				
				if (!response.succeeded()) {
					throw new TransportException(response.getError().getMessage());
				} 
				log.info("Authentication succeeded ('" + response.getEntity().getMessage() + "')");
				connectLatch.countDown();
			} catch (Exception e) {
				log.error("Authentication failed", e);
			}
		}, 100, 5000, TimeUnit.MILLISECONDS);
		try {
			if (connectLatch.await(timeout, TimeUnit.MILLISECONDS)) {
				success = true;
			} else {
				log.error("Aborting authentication attempt after " + timeout + "ms." );
			}
			reconnectFuture.cancel(true);
			reconnectExecutor.shutdown();
		} catch (InterruptedException e) {
			log.error("Authentication attempt interrupted", e);
		}
		this.isAuthenticated = success;
		return success;
	}
	
	public boolean isConnectedToPushService() {
		return pushTransport != null && pushTransport.isConnected();
	}
	
	public boolean connectToPushService() throws TransportException {
		return connectToPushService(DEFAULT_CONNECT_TIMEOUT_MS);
	}
	
	public boolean connectToPushService(long timeout) throws TransportException {
		if (!isAuthenticated()) throw new TransportException("Client has not authenticated with the server");
		if (pushTransport != null) {
			pushTransport.disconnect();
		}
		pushTransport = new WAsyncTransport();
		pushTransport.addPushEventListener(this);
		final CountDownLatch connectLatch = new CountDownLatch(1);
		final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
		final ScheduledFuture<?> reconnectFuture = reconnectExecutor.scheduleAtFixedRate(() -> {
			try {
				log.info("Connecting to the Nimbus push service...");
				pushTransport.connect(getAsyncRequest());
				log.info("Connected to the Nimbus push service");
				connectLatch.countDown();
			} catch (Exception e) {
				log.error("Connection attempt failed", e);
			}
		}, 0, 5, TimeUnit.SECONDS);
		try {
			if (!connectLatch.await(timeout, TimeUnit.MILLISECONDS)) {
				log.error("Aborting connection attempt after " + timeout + "ms." );
			}
			reconnectFuture.cancel(true);
			reconnectExecutor.shutdown();
		} catch (InterruptedException e) {
			log.error("Connection attempt interrupted", e);
		}
		return pushTransport.isConnected();
	}
	
	public void disconnect() {
		log.info("Disconnecting from async event bus.");
		if (pushTransport != null) {
			pushTransport.disconnect();
			pushTransport = null;
		}
		if (transport != null) {
			transport.close();
			transport = null;
		}
	}
	
	public FileSyncManager getFileSyncManager() {
		return new FileSyncManager(this);
	}
	
	public <U, T> NimbusResponse<T> process(NimbusRequest<U, T> request) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (transport == null) transport = new JerseyTransport();
		return transport.process(originationIdProvider.setOriginationId(request));
	}
	
	public <U, T> NimbusResponse<T> process(NimbusRequest<U, T> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (transport == null) transport = new JerseyTransport();
		return transport.process(originationIdProvider.setOriginationId(request), readTimeout);
	}

	public NimbusResponse<File> processDownload(NimbusRequest<Void, File> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (transport == null) transport = new JerseyTransport();
		return transport.download(originationIdProvider.setOriginationId(request), readTimeout);
	}

	public NimbusResponse<Void> processUpload(NimbusRequest<File, Void> request, int readTimeout) throws InvalidRequestException, InvalidResponseException, TransportException {
		if (transport == null) transport = new JerseyTransport();
		return transport.upload(originationIdProvider.setOriginationId(request), readTimeout);
	}
	
	public void addPushEventListener(PushEventListener listener) {
		pushEventListeners.add(listener);
	}
	
	public void removePushEventListener(PushEventListener listener) {
		pushEventListeners.remove(listener);
	}
	
	@Override
	public void onClose(PushTransport transport) {
		for (PushEventListener listener : pushEventListeners) {
			listener.onClose(transport);
		}
	}

	@Override
	public void onConnect(PushTransport transport) {
		for (PushEventListener listener : pushEventListeners) {
			listener.onConnect(transport);
		}
	}
	
	@Override
	public void onFileEvent(PushTransport transport, FileEvent event) {
		for (PushEventListener listener : pushEventListeners) {
			if (originationIdFilter == null 
					|| !originationIdFilter.isFiltered(event.getOriginationId())) {
				log.debug("File event recieved: {}", event);
				listener.onFileEvent(transport, event);
			} else {
				log.debug("Filtered pushed file event which originated from this client: {}", event);
			}
		}
	}
	
	@Override
	public void onSyncRootChangeEvent(PushTransport transport, SyncRootChangeEvent event) {
		log.debug("Sync root change event recieved: {}", event);
		for (PushEventListener listener : pushEventListeners) {
			listener.onSyncRootChangeEvent(transport, event);
		}
	}
	
	@Override
	public void onError(PushTransport transport, Throwable t) {
		for (PushEventListener listener : pushEventListeners) {
			listener.onError(transport, t);
		}
	}
	
	private NimbusRequest<Void, AuthenticateResponse> getAuthenticateRequest() {
		return new NimbusRequest<>(
				credentials, 
				NimbusRequest.Method.GET, 
				getApiEndpoint(), 
				"/authenticate",
				new GenericType<AuthenticateResponse>(){}
			);
	}
	
	private NimbusRequest<Void, Void> getAsyncRequest() {
		return new NimbusRequest<>(
				credentials, 
				NimbusRequest.Method.GET, 
				getAsyncEndpoint(), 
				"/eventbus",
				new GenericType<Void>(){}
			);
	}
	
	public void setOriginationIdProvider(OriginationIdProvider provider) {
		this.originationIdProvider = provider;
	}
	
	public void addOriginationIdListener(OriginationIdListener listener) {
		originationIdListeners.add(listener);
	}
	
	public void removeOriginationIdListener(OriginationIdListener listener) {
		originationIdListeners.remove(listener);
	}
	
	public void enablePushEventOriginationFilter() {
		originationIdFilter = new OriginationIdFilter(this);
	}
	
	public void disablePushEventOriginationFilter() {
		originationIdFilter = null;
	}
	
	@Override
	public void onOriginationIdProvided(NimbusRequest<?, ?> request) {
		for (OriginationIdListener listener : originationIdListeners) {
			listener.onOriginationIdProvided(request);
		}
	}
}
