package com.kbdunn.nimbus.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.atmosphere.container.Jetty9AsyncSupportWithWebSocket;
import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.SessionSupport;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.bridge.SLF4JBridgeHandler;

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
		//Appender file = LogManager.getRootLogger().getAppender("FILE");
		//log.info("FILE appender is " + file);
		//log.info(((RollingFileAppender) file).getFile());
		/*ConsoleAppender console = new ConsoleAppender(); 
		//configure the appender
		String pattern = "%d [%p|%c|%C{1}] %m%n";
		console.setLayout(new PatternLayout(pattern)); 
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
		//add appender to any Logger (here is root)
		Logger.getRootLogger().addAppender(console);
		
		//System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.selector.BasicContextSelector");
		RollingFileAppender file = new RollingFileAppender();
		file.setName("FILE");
		file.setFile(ClassLoader.getSystemClassLoader().getResource(".").getPath() + "../logs/nimbus.log");
		file.setLayout(new PatternLayout("%d %-5p [%c{1}] %m%n"));
		file.setThreshold(Level.DEBUG);
		file.setMaxBackupIndex(2);
		file.setMaxFileSize("5MB");
		file.setAppend(true);
		file.activateOptions();
		Logger.getRootLogger().addAppender(file);
		Logger.getRootLogger().setLevel(Level.DEBUG);
		Logger.getLogger("com.kbdunn").setLevel(Level.DEBUG);
		Logger.getLogger("org.apache").setLevel(Level.INFO);
		Logger.getLogger("org.atmosphere").setLevel(Level.WARN);
		Logger.getLogger("com.vaadin").setLevel(Level.INFO);
		Logger.getLogger("org.jaudiotagger").setLevel(Level.INFO);*/
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
		log.info("Listening on http://localhost:" + port);
		log.info("-------------------------------------------------");
		
		Boolean sslEnabled = NimbusContext.instance().getPropertiesService().isSslEnabled();
		if (sslEnabled == null) sslEnabled = false;
		Integer httpsPort = NimbusContext.instance().getPropertiesService().getHttpsPort();
		
		final Server server = new Server();
		
		// HTTP Configuration
		HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(8443);
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
			log.info("SSL is enabled.");
			
			String keystore = NimbusContext.instance().getPropertiesService().getKeystorePath();
			String keystorePw = NimbusContext.instance().getPropertiesService().getKeystorePassword();
			String truststore = NimbusContext.instance().getPropertiesService().getTruststorePath();
			String truststorePw = NimbusContext.instance().getPropertiesService().getTruststorePassword();
			String keymanagerPw = NimbusContext.instance().getPropertiesService().getKeyManagerPassword();
			
			log.info("Configuring SSL over port " + httpsPort);
			
			if (keystore == null || keystore.isEmpty() || keystorePw == null || keystorePw.isEmpty()) {
				http.close();
				throw new IllegalStateException("The nimbus.ssl.keystore.path and nimbus.ssl.keystore.password properties must be set to enable SSL.");
			}
			SslContextFactory sslContextFactory = new SslContextFactory(keystore);
			sslContextFactory.setKeyStorePassword(keystorePw);
			sslContextFactory.setIncludeCipherSuites(".*RC4.*"); // Prevent BEAST attack?
			
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
			
			// Causes http requests to return a !403 error, effectively redirecting to https
			ConstraintSecurityHandler constraintHandler = new ConstraintSecurityHandler();
			Constraint constraint = new Constraint();
			constraint.setDataConstraint(Constraint.DC_CONFIDENTIAL);
			ConstraintMapping constraintMapping = new ConstraintMapping();
			constraintMapping.setPathSpec("/*");
			constraintMapping.setConstraint(constraint);
			constraintHandler.addConstraintMapping(constraintMapping);
			server.setHandler(constraintHandler);
			
			server.setConnectors(new Connector[] { http, https });
		} else {
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
		
		final WebAppContext webAppContext = new WebAppContext();
		webAppContext.setDefaultsDescriptor(null); // Disable JSP Support (slows down startup)
		webAppContext.setContextPath("/");
		webAppContext.setParentLoaderPriority(true);
		final ServletContextHandler contextHandler = (ServletContextHandler) webAppContext.getServletContext().getContextHandler();
		contextHandler.setMaxFormKeys(Integer.MAX_VALUE);
		contextHandler.setMaxFormContentSize(Integer.MAX_VALUE);
		
		// Set Nimbus services
		contextHandler.setAttribute(AsyncService.class.getName(), NimbusContext.instance().getAsyncService());
		contextHandler.setAttribute(FileService.class.getName(), NimbusContext.instance().getFileService());
		contextHandler.setAttribute(FileShareService.class.getName(), NimbusContext.instance().getFileShareService());
		contextHandler.setAttribute(MediaLibraryService.class.getName(), NimbusContext.instance().getMediaLibraryService());
		contextHandler.setAttribute(PropertiesService.class.getName(), NimbusContext.instance().getPropertiesService());
		contextHandler.setAttribute(StorageService.class.getName(), NimbusContext.instance().getStorageService());
		contextHandler.setAttribute(UserService.class.getName(), NimbusContext.instance().getUserService());
		contextHandler.setAttribute(OAuthService.class.getName(), NimbusContext.instance().getOAuthService());
		
		// Setup Vaadin Servlet
		ServletHolder vaadinServletHolder = new ServletHolder(new NimbusVaadinServlet());
		vaadinServletHolder.setInitOrder(1);
		vaadinServletHolder.setInitParameter("application", "Nimbus");
		vaadinServletHolder.setInitParameter("pushmode", "manual");
		vaadinServletHolder.setAsyncSupported(true);
		webAppContext.addServlet(vaadinServletHolder, "/*");
		if (NimbusContext.instance().getPropertiesService().isDevMode()) {
			webAppContext.setResourceBase(System.getProperty("nimbus.home") + "/webapp/src/main/webapp");
		} else {
			webAppContext.setResourceBase(System.getProperty("nimbus.home") + "/static");
		}
		webAppContext.addEventListener(new SessionSupport()); // Needed for Atmosphere (Vaadin push)
		
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
		
		// Setup VAADIN resource handler 
		/*final ResourceHandler vaadinResourceHandler = new ResourceHandler();
		vaadinResourceHandler.setDirectoriesListed(true);
		vaadinResourceHandler.setResourceBase(System.getProperty("nimbus.home") + "/src/main/webapp");*/
		
		// Setup Jersey servlet
		final ResourceConfig resourceConfig = new ResourceConfig()
				.packages("com.kbdunn.nimbus.server.api")
				.packages("com.kbdunn.nimbus.server.api.resources");
		EncodingFilter.enableFor(resourceConfig, GZipEncoder.class);
		final ServletHolder jerseyServletHolder = new ServletHolder(new ServletContainer(resourceConfig));
		jerseyServletHolder.setInitOrder(2);
		contextHandler.addServlet(jerseyServletHolder, "/request/*");
		
		// Setup Atmosphere servlet
		final ServletHolder atmosphereServletHolder = new ServletHolder(AtmosphereServlet.class);
		atmosphereServletHolder.setInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, "com.kbdunn.nimbus.server.api.async");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.DEFAULT_CONTENT_TYPE, "application/json");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.SCAN_CLASSPATH, "false");
		atmosphereServletHolder.setInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, Jetty9AsyncSupportWithWebSocket.class.getName());
		atmosphereServletHolder.setAsyncSupported(true);
		contextHandler.addServlet(atmosphereServletHolder, "/async/*");
		
		// Set server handlers
		server.setHandler(contextHandler);//webAppContext);
		
		try {
			runDataPrimers();
			server.start();
			server.join();
			log.info("Server started. Application available at: " + "http://localhost:" + port);
			// Make AtmosphereFramework available in NimbusContext
			final AtmosphereServlet atmosphereServlet = (AtmosphereServlet) atmosphereServletHolder.getServlet();
			NimbusContext.instance().setAtmosphereFramework(atmosphereServlet.framework());
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