package com.kbdunn.nimbus.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map.Entry;
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
import com.kbdunn.nimbus.common.server.NimbusphereService;
import com.kbdunn.nimbus.common.server.OAuthService;
import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.common.server.StorageService;
import com.kbdunn.nimbus.common.server.UserService;
import com.kbdunn.nimbus.server.async.ReconcileOperation;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.security.SecuredRedirectHandler;
import com.kbdunn.nimbus.server.service.LocalNimbusphereService;
import com.kbdunn.nimbus.server.util.DatabaseCleaner;
import com.kbdunn.nimbus.server.util.DemoModePrimer;
import com.kbdunn.nimbus.server.util.DevModePrimer;
import com.kbdunn.nimbus.web.NimbusVaadinServlet;

public class Launcher {
	
	private static final Logger log = Logger.getLogger(Launcher.class.getName());
	private static final int SHUTDOWN_PORT = 8081;
	
	private static Launcher instance;
	
	private Server server;
	private Integer port;
	//private boolean restart = false;
	
	private HandlerList handlers;
	private WebAppContext webappContext;
	private ServletHolder vaadinServletHolder;
	private ServletHolder jerseyServletHolder;
	private ServletHolder atmosphereServletHolder;
	
	public static void main(String[] args) throws Exception {
		System.setProperty("java.awt.headless", "true");
		instance = new Launcher();
		instance.launch();
	}
	
	public static Launcher instance() {
		return instance;
	}

	public Launcher() {  }
	
	private void launch() {
		port = NimbusContext.instance().getPropertiesService().getHttpPort();
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
		
		try {
			configureLog4j();
			shutdownServer();
			initializeDatabase();
			runDataPrimers();
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
	
	private void initializeDatabase() {
		// Clean database if DEV or DEMO mode
		if (NimbusContext.instance().getPropertiesService().isDevMode() || NimbusContext.instance().getPropertiesService().isDemoMode()) {
			log.info("Cleaning database...");
			new DatabaseCleaner().rebuild();
		}
		// Initialize Connection Pool
		HikariConnectionPool.init();
		log.info("Hikari Connection Pool initialized");
	}
	
	private void shutdownServer() {
		if (!NimbusContext.instance().getPropertiesService().isDevMode()) return;
		
		// Try to shutdown existing process, if running
		try {
            Socket socket = new Socket((String) null, SHUTDOWN_PORT);
            socket.getInputStream().read();
            socket.close();
        } catch (IOException e) {
            // Ignore if port is not open (another instance isn't running)
        }
	}
	
	private void runServer() throws Exception {		
		
		//restart = false; // Clear restart flag, if set
		
		Boolean sslEnabled = NimbusContext.instance().getPropertiesService().isSslEnabled();
		if (sslEnabled == null) sslEnabled = false;
		Integer httpsPort = NimbusContext.instance().getPropertiesService().getHttpsPort();
		
		server = new Server();
		
		// Initialize handler list
		handlers = new HandlerList();
		
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
		
		this.buildWebappContext();
		handlers.addHandler(webappContext.getServletContext().getContextHandler());
		server.setHandler(handlers);
		
		// Enable auto-reload if running in dev mode
		// Couldn't get this to work for the life of me
		/*if (NimbusContext.instance().getPropertiesService().isDevMode()) {
            int interval = 1;
            List<File> classFolders = new ArrayList<File>();
            ClassLoader cl = server.getClass().getClassLoader();
            for (URL u : ((URLClassLoader) cl).getURLs()) {
                File f = new File(u.getPath());
                if (f.isDirectory()) {
                    classFolders.add(f);
                }
            }
            classFolders.add(new File("src/main/webapp/VAADIN/themes"));
            
            log.debug("Enabling context auto-reload. Scan interval is " + interval + " second(s). Scanned folders are: ");
            for (File f : classFolders) {
                log.debug("  " + f.getAbsolutePath());
                webappContext.setExtraClasspath(f.getAbsolutePath());
            }
            
            Scanner scanner = new Scanner();
            scanner.setScanInterval(interval);

            scanner.setRecursive(true);
            scanner.addListener(new Scanner.BulkListener() {
                @Override
                public void filesChanged(List<String> filenames) throws Exception {
                	log.debug("File change detected. Restarting server");
                	for (String filename : filenames) {
                		//if (!filename.endsWith(".class") || filename.contains("$")) continue;
                		filename = filename.replace("\\", "/");
                		String binaryName = filename
                				.substring(filename.indexOf("/bin/") + 5)
                				.replace("/", ".")
                				.replace(".class", "");
                		log.debug("Reloading class: " + binaryName);
                		webappContext.getClassLoader().loadClass(binaryName);
                		for (Handler handler : handlers.getChildHandlers()) {
                			handler.getClass().getClassLoader().loadClass(binaryName);
                		}
                		instance.getClass().getClassLoader().loadClass(binaryName);
                	} 
                	restart = true;
                	server.stop();
                	handlers.stop();
                	for (Handler handler : handlers.getChildHandlers()) {
                		handlers.removeHandler(handler);
                	}
                	handlers = new HandlerList();
                	buildWebappContext();
                	handlers.addHandler(webappContext.getServletContext().getContextHandler());
                	server.stop();
                	server.setHandler(handlers);
                	server.start();
                	//handlers.start();
                }
            });
            scanner.setReportExistingFilesOnStartup(false);
            scanner.setFilenameFilter(new FilenameFilter() {
                @Override
                public boolean accept(File folder, String name) {
                    return name.endsWith(".class") || name.endsWith(".css");
                }
            });

            scanner.setScanDirs(classFolders);
            scanner.start();
            server.addBean(scanner);
        }*/
		
		try {
			server.start();
			if (!NimbusContext.instance().getPropertiesService().isDemoMode()) {
				// Make AtmosphereFramework available in NimbusContext
				final AtmosphereServlet atmosphereServlet = (AtmosphereServlet) atmosphereServletHolder.getServlet();
				NimbusContext.instance().setAtmosphereFramework(atmosphereServlet.framework());
			}

			if (NimbusContext.instance().getPropertiesService().isDevMode()) {
				// Listen on shutdown port
				this.listenOnShutdownPort(server);
			}
			server.join();
		} finally {
			server.stop();
			HikariConnectionPool.destroy();
			/*if (restart) {
				this.runServer();
			}*/
		}
	}
	
	private void buildWebappContext() {
		webappContext = new WebAppContext();
		webappContext.setDefaultsDescriptor(null); // Disable JSP Support (slows down startup)
		webappContext.setContextPath("/");
		webappContext.setParentLoaderPriority(true);
		
		final ServletContextHandler webappContextHandler = (ServletContextHandler) webappContext.getServletContext().getContextHandler();
		webappContextHandler.setMaxFormKeys(Integer.MAX_VALUE);
		webappContextHandler.setMaxFormContentSize(Integer.MAX_VALUE);
		
		// Set Nimbus services
		webappContextHandler.setAttribute(AsyncService.class.getName(), NimbusContext.instance().getAsyncService());
		webappContextHandler.setAttribute(FileService.class.getName(), NimbusContext.instance().getFileService());
		webappContextHandler.setAttribute(FileShareService.class.getName(), NimbusContext.instance().getFileShareService());
		webappContextHandler.setAttribute(MediaLibraryService.class.getName(), NimbusContext.instance().getMediaLibraryService());
		webappContextHandler.setAttribute(PropertiesService.class.getName(), NimbusContext.instance().getPropertiesService());
		webappContextHandler.setAttribute(StorageService.class.getName(), NimbusContext.instance().getStorageService());
		webappContextHandler.setAttribute(UserService.class.getName(), NimbusContext.instance().getUserService());
		webappContextHandler.setAttribute(OAuthService.class.getName(), NimbusContext.instance().getOAuthService());
		webappContextHandler.setAttribute(NimbusphereService.class.getName(), NimbusContext.instance().getNimbusphereService());
		
		// Start Nimbusphere heartbeat
		((LocalNimbusphereService) NimbusContext.instance().getNimbusphereService()).startHeartbeat();
		
		// Setup Vaadin Servlet
		vaadinServletHolder = new ServletHolder(new NimbusVaadinServlet());
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
		
		// Do not enable APIs for the demo
		if (!NimbusContext.instance().getPropertiesService().isDemoMode()) {
			// Setup Jersey servlet
			final JacksonJaxbJsonProvider jsonProvider = new JacksonJaxbJsonProvider();
			jsonProvider.setMapper(ObjectMapperSingleton.getMapper());
			final ResourceConfig resourceConfig = new ResourceConfig()
					.packages("com.kbdunn.nimbus.server.api")
					.packages("com.kbdunn.nimbus.server.api.resources")
					.register(jsonProvider)
					.register(MultiPartFeature.class);
			EncodingFilter.enableFor(resourceConfig, GZipEncoder.class);
			jerseyServletHolder = new ServletHolder(new ServletContainer(resourceConfig));
			jerseyServletHolder.setInitOrder(2);
			webappContextHandler.addServlet(jerseyServletHolder, "/request/*");
			
			// Setup Atmosphere servlet
			atmosphereServletHolder = new ServletHolder(AtmosphereServlet.class);
			atmosphereServletHolder.setInitParameter(ApplicationConfig.ANNOTATION_PACKAGE, "com.kbdunn.nimbus.server.api.async");
			atmosphereServletHolder.setInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json");
			atmosphereServletHolder.setInitParameter(ApplicationConfig.PROPERTY_COMET_SUPPORT, Jetty9AsyncSupportWithWebSocket.class.getName());
			atmosphereServletHolder.setAsyncSupported(true);
			atmosphereServletHolder.setInitOrder(1);
			webappContextHandler.addServlet(atmosphereServletHolder, "/async/*");
		}
	}
	
	private void listenOnShutdownPort(Server server) throws UnknownHostException, IOException {
        final ServerSocket serverSocket = new ServerSocket(SHUTDOWN_PORT, 1, InetAddress.getByName("127.0.0.1"));
        if (serverSocket.isBound()) return;
        new Thread() {
            @Override
            public void run() {
                try {
                    log.debug("Waiting for shutdown signal on port " + serverSocket.getLocalPort());
                    // Start waiting for a close signal
                    Socket accept = serverSocket.accept();
                    // First stop listening to the port
                    serverSocket.close();
                    
                    // Start a thread that kills the JVM if server.stop() doesn't have any effect
                    Thread interruptThread = new Thread() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(5000);
                                if (!server.isStopped()) {
                                    log.debug("Jetty still running. Closing JVM.");
                                    dumpThreadStacks();
                                    System.exit(-1);
                                }
                            } catch (InterruptedException e) {
                                // Interrupted if server.stop() was successful
                            }
                        }
                    };
                    interruptThread.setDaemon(true);
                    interruptThread.start();

                    // Then stop the jetty server
                    server.stop();

                    interruptThread.interrupt();

                    // Send a byte to tell the other process that it can start jetty
                    OutputStream outputStream = accept.getOutputStream();
                    outputStream.write(0);
                    outputStream.flush();
                    // Finally close the socket
                    accept.close();
                } catch (Exception e) {
                    log.error(e, e);
                }
            }
        }.start();
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
	
    private static void dumpThreadStacks() {
        for (Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            Thread thread = entry.getKey();
            StackTraceElement[] stackTraceElements = entry.getValue();

            log.debug(thread.getName() + " - " + thread.getState());
            for (StackTraceElement stackTraceElement : stackTraceElements) {
            	log.debug("    at " + stackTraceElement.toString());
            }
        }
    }
}