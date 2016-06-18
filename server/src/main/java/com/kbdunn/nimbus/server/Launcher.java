package com.kbdunn.nimbus.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.atmosphere.container.Jetty9AsyncSupportWithWebSocket;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.SessionSupport;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.kbdunn.nimbus.api.network.jackson.ObjectMapperSingleton;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.common.server.AsyncService;
import com.kbdunn.nimbus.common.server.FileService;
import com.kbdunn.nimbus.common.server.FileShareService;
import com.kbdunn.nimbus.common.server.MediaLibraryService;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.server.async.ReconcileOperation;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.security.SecuredRedirectHandler;
import com.kbdunn.nimbus.server.util.DatabaseCleaner;
import com.kbdunn.nimbus.server.util.DemoModePrimer;
import com.kbdunn.nimbus.server.util.DevModePrimer;
import com.kbdunn.nimbus.web.NimbusVaadinServlet;

public class Launcher {
	
	private static final Logger log = Logger.getLogger(Launcher.class.getName());
	
	private static Launcher instance;
	
	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true");
		if (instance != null) {
			System.err.println("Nimbus is already running!");
			System.exit(1);
		}
		instance = new Launcher();
		instance.launch();
	}
	
	public static Launcher instance() {
		return instance;
	}

	public Launcher() {  }
	
	private void launch() {
		try {
			configureLog4j();
			runServer();
		} catch (Exception e) {
			log.error(e, e);
			System.exit(1);
		}
	}
	
	private void configureLog4j() {
		LogManager.getRootLogger().removeAllAppenders(); // Clear log4j configuration
		java.util.logging.LogManager.getLogManager().reset(); // Clear JUL configuration
		SLF4JBridgeHandler.install(); // Enable slf4j bridges
		java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST); // Set JUL to FINEST (slf4j then filters messages)
		//Logger.getRootLogger().getLoggerRepository().resetConfiguration(); // remove any existing appenders
		Properties log4jprops = new Properties();
		InputStream is = null;
		try {
			if (NimbusContext.instance().getPropertiesService().isDevMode()) {
				is = new FileInputStream(new File(System.getProperty("nimbus.home") + "/server/src/main/resources/log4j.properties"));
			} else {
				is = new FileInputStream(new File(System.getProperty("nimbus.home") + "/conf/log4j.properties"));
			}
			log4jprops.load(is);
		} catch (IOException e) {
			log.error(e, e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		PropertyConfigurator.configure(log4jprops);
	}
	
	/**
	 * @throws Exception
	 */
	private void runServer() throws Exception {		
		Integer port = NimbusContext.instance().getPropertiesService().getHttpPort();
		if (port == null) {
			log.warn("The nimbus.http.port property is not set. Defaulting to 8080.");
			port = 8080;
		}
		
		String mode = NimbusContext.instance().getPropertiesService().isDemoMode() ? " in DEMO mode" 
				: NimbusContext.instance().getPropertiesService().isDevMode() ? " in DEV mode" : "";
		
		log.info("-------------------------------------------------");
		log.info("Starting Nimbus" + mode);
		log.info("Nimbus Home: " + System.getProperty("nimbus.home"));
		log.info("Listening on http://localhost:" + port);
		log.info("-------------------------------------------------");
		
		Boolean sslEnabled = NimbusContext.instance().getPropertiesService().isSslEnabled();
		if (sslEnabled == null) sslEnabled = false;
		Integer httpsPort = NimbusContext.instance().getPropertiesService().getHttpsPort();
		
		final Server server = new Server();
		
		// Initialize handler list
		HandlerList handlers = new HandlerList();
		
		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(65536);
        httpConfig.setRequestHeaderSize(16384);
        httpConfig.setResponseHeaderSize(16384);
        httpConfig.setSendServerVersion(true);
        httpConfig.setSendDateHeader(false);
        httpConfig.addCustomizer(new ForwardedRequestCustomizer()); // Handle proxies/load balancers
        if (sslEnabled) {
        	httpConfig.setSecureScheme("https");
        	if (httpsPort == null) {
				log.warn("The nimbus.ssl.https.port property is not set. Defaulting to 8443.");
				httpsPort = 8443;
			}
        	httpConfig.setSecurePort(httpsPort);
            httpConfig.addCustomizer(new SecureRequestCustomizer()); 
        }
		
        // HTTP Connector
		final ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
		http.setIdleTimeout(30000);
		http.setPort(port);
		
		// SSL
		ServerConnector https = null;
		if (sslEnabled) {
			String keystore = NimbusContext.instance().getPropertiesService().getKeystorePath();
			String keystorePw = NimbusContext.instance().getPropertiesService().getKeystorePassword();
			String truststore = NimbusContext.instance().getPropertiesService().getTruststorePath();
			String truststorePw = NimbusContext.instance().getPropertiesService().getTruststorePassword();
			String keymanagerPw = NimbusContext.instance().getPropertiesService().getKeyManagerPassword();
			
			if (keystore == null || keystore.isEmpty() || keystorePw == null || keystorePw.isEmpty()) {
				http.close();
				throw new IllegalStateException("The nimbus.ssl.keystore.path and nimbus.ssl.keystore.password properties must be set to enable SSL.");
			}
			
			log.info("Enabling SSL over port " + httpsPort + " using keystore located at " + keystore);
			
			SslContextFactory sslContextFactory = new SslContextFactory(keystore);
			sslContextFactory.setKeyStorePassword(keystorePw);
			//sslContextFactory.setIncludeCipherSuites(".*RC4.*"); // Prevent BEAST attack?
			
			if (truststore != null && !truststore.isEmpty()) {
				sslContextFactory.setTrustStorePath(truststore);
				sslContextFactory.setTrustStorePassword(truststorePw);
			}
			
			if (keymanagerPw != null && !keymanagerPw.isEmpty()) {
				sslContextFactory.setKeyManagerPassword(keymanagerPw);
			}
			
			HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
			httpsConfig.addCustomizer(new SecureRequestCustomizer());
			
			https = new ServerConnector(server, 
					new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), 
					new HttpConnectionFactory(httpsConfig));
			https.setPort(httpsPort);
			https.setIdleTimeout(500000);
			
			// Redirect http to https
			handlers.addHandler(new SecuredRedirectHandler());
			
			// Add HTTP and HTTPS connectors
			server.setConnectors(new Connector[] { http, https });
		} else {
			// Add HTTP connector
			server.setConnectors(new Connector[] { http });
		}
		
		// Clean database if DEV or DEMO mode
		if (NimbusContext.instance().getPropertiesService().isDevMode() || NimbusContext.instance().getPropertiesService().isDemoMode()) {
			log.info("Cleaning database...");
			new DatabaseCleaner().rebuild();
		}
		// Initialize Connection Pool
		HikariConnectionPool.init();
		log.info("Hikari Connection Pool initialized");
		
		final WebAppContext webappContext = new WebAppContext();
		webappContext.setDefaultsDescriptor(null); // Disable JSP Support (slows down startup)
		webappContext.setContextPath("/");
		webappContext.setParentLoaderPriority(true);
		final ServletContextHandler webappContextHandler = (ServletContextHandler) webappContext.getServletContext().getContextHandler();
		webappContextHandler.setMaxFormKeys(Integer.MAX_VALUE);
		webappContextHandler.setMaxFormContentSize(Integer.MAX_VALUE);
		handlers.addHandler(webappContextHandler);
		
		// Set Nimbus services
		webappContextHandler.setAttribute(AsyncService.class.getName(), NimbusContext.instance().getAsyncService());
		webappContextHandler.setAttribute(FileService.class.getName(), NimbusContext.instance().getFileService());
		webappContextHandler.setAttribute(FileShareService.class.getName(), NimbusContext.instance().getFileShareService());
		webappContextHandler.setAttribute(MediaLibraryService.class.getName(), NimbusContext.instance().getMediaLibraryService());
		webappContextHandler.setAttribute(PropertiesService.class.getName(), NimbusContext.instance().getPropertiesService());
		webappContextHandler.setAttribute(StorageService.class.getName(), NimbusContext.instance().getStorageService());
		webappContextHandler.setAttribute(UserService.class.getName(), NimbusContext.instance().getUserService());
		webappContextHandler.setAttribute(OAuthService.class.getName(), NimbusContext.instance().getOAuthService());
		
		// Setup Vaadin Servlet
		ServletHolder vaadinServletHolder = new ServletHolder(new NimbusVaadinServlet());
		vaadinServletHolder.setInitOrder(1);
		vaadinServletHolder.setInitParameter("application", "Nimbus");
		vaadinServletHolder.setInitParameter("pushmode", "manual");
		vaadinServletHolder.setAsyncSupported(true);
		webappContext.addServlet(vaadinServletHolder, "/*");
		if (NimbusContext.instance().getPropertiesService().isDevMode()) {
			webappContext.setResourceBase(System.getProperty("nimbus.home") + "/webapp/src/main/webapp");
		} else {
			webappContext.setResourceBase(System.getProperty("nimbus.home") + "/static");
		}
		webappContext.addEventListener(new SessionSupport()); // Needed for Atmosphere (Vaadin push)
		
		// Configure MIME types
		/*MimeTypes mimeTypes = webAppContext.getMimeTypes();
		mimeTypes.addMimeMapping("ogg", "audio/ogg");
		mimeTypes.addMimeMapping("ogv", "video/ogg");
		mimeTypes.addMimeMapping("mp4", "video/mp4");
		mimeTypes.addMimeMapping("webm", "video/webm");
		mimeTypes.addMimeMapping("wmv", "video/x-ms-wmv");
		mimeTypes.addMimeMapping("wma", "audio/x-ms-wma");
		mimeTypes.addMimeMapping("flv", "video/x-flv");
		mimeTypes.addMimeMapping("avi", "video/x-msvideo");
		webAppContext.setMimeTypes(mimeTypes);
		contextHandler.setMimeTypes(mimeTypes);*/
		
		// Setup Jersey servlet
		final JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
		jsonProvider.setMapper(ObjectMapperSingleton.getMapper());
		final ResourceConfig resourceConfig = new ResourceConfig()
				.packages("com.kbdunn.nimbus.server.api")
				.packages("com.kbdunn.nimbus.server.api.resources")
				.register(jsonProvider)
				.register(MultiPartFeature.class);
		EncodingFilter.enableFor(resourceConfig, GZipEncoder.class);
		final ServletHolder jerseyServletHolder = new ServletHolder(new ServletContainer(resourceConfig));
		jerseyServletHolder.setInitOrder(2);
		webappContextHandler.addServlet(jerseyServletHolder, "/request/*");
		
		// Setup Atmosphere servlet
		final ServletHolder atmosphereServletHolder = new ServletHolder(AtmosphereServlet.class);
		atmosphereServletHolder.setInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, "com.kbdunn.nimbus.server.api.async");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.PROPERTY_COMET_SUPPORT, Jetty9AsyncSupportWithWebSocket.class.getName());
		atmosphereServletHolder.setAsyncSupported(true);
		atmosphereServletHolder.setInitOrder(1);
		webappContextHandler.addServlet(atmosphereServletHolder, "/async/*");
		
		// Set server handlers
		server.setHandler(handlers);
		
		try {
			runDataPrimers();
			server.start();
			// Make AtmosphereFramework available in NimbusContext
			final AtmosphereServlet atmosphereServlet = (AtmosphereServlet) atmosphereServletHolder.getServlet();
			NimbusContext.instance().setAtmosphereFramework(atmosphereServlet.framework());
			server.join();
		} finally {
			server.stop();
			HikariConnectionPool.destroy();
		}
	}
	
	private void runDataPrimers() {		
		if (NimbusContext.instance().getPropertiesService().isDemoMode()) {
        	new DemoModePrimer().go();
        } else if (NimbusContext.instance().getPropertiesService().isDevMode()) {
        	new DevModePrimer().go();
		} else {
			NimbusContext.instance().getStorageService().scanAndMountUSBHardDrives(); // Update connected drives
        }
		NimbusContext.instance().getStorageService().resetReconciliation(); // Set reconciliation flags to false
		if (!NimbusContext.instance().getPropertiesService().isDevMode()) {
	        for (StorageDevice d : NimbusContext.instance().getStorageService().getAssignedStorageDevices()) {
	        	if (NimbusContext.instance().getStorageService().storageDeviceIsAvailable(d)) {
	        		new Thread(new ReconcileOperation(null, d)).start();
	        	}
	        }
		}
	}
}