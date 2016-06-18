package com.kbdunn.nimbus.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Launcher {
	
	private static Logger log = LoggerFactory.getLogger(Launcher.class);
	
	public static void main(String[] args) {
		try {
			ApplicationProperties.instance().initialize();
			configureLog4j();
			log.info("Starting Nimbus Sync");
			loadSwtJar();
			Thread.currentThread().setName("Main Thread");
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				Application.shutdown();
			}));
			Application.initialize().launch();
		} catch (Exception e) {
			log.error("Uncaught error!", e);
			System.exit(1);
		}
	}
	
	private static void loadSwtJar() {
	    try {
	        Class.forName("org.eclipse.swt.widgets.Display");
	        log.warn("An existing classpath entry was found for SWT!");
	        return;
	    } catch (ClassNotFoundException e) {
	        // Expected
	    }
	    
	    String osName = System.getProperty("os.name").toLowerCase();
	    String osArch = System.getProperty("os.arch").toLowerCase();
	    
	    String osPart = 
	        osName.contains("win") ? "win" :
	        osName.contains("mac") ? "cocoa" :
	        osName.contains("linux") || osName.contains("nix") ? "gtk" :
	        null;
	    
	    if (null == osPart)
	        throw new RuntimeException ("Cannot determine correct SWT jar from os.name [" + osName + "] and os.arch [" + osArch + "]");
	    
	    String archPart = osArch.contains("64") ? "64" : "32";
	    log.info("Detected "+archPart+"bit "+osPart + " OS and architecture");
	    
	    String swtFileName = "swt_" +osPart + archPart +".jar";
	    File file = new File(ApplicationProperties.instance().getInstallDirectory(), "lib/" + swtFileName);
	    if (!file.exists()) {
	        log.error("Could not locate the SWT jar " + file.getAbsolutePath());
	    }
	    
	    try {
	        URLClassLoader classLoader = (URLClassLoader) Application.class.getClassLoader();
	        Method addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
	        addUrlMethod.setAccessible(true);
	        URL swtFileUrl = file.toURI().toURL();
	        addUrlMethod.invoke(classLoader, swtFileUrl);
	    }
	    catch (Exception e) {
	        throw new RuntimeException("Unable to add the swt jar to the class path: " + file.getAbsoluteFile(), e);
	    }
	}
	
	private static void configureLog4j() {
		LogManager.getRootLogger().removeAllAppenders(); // Clear log4j configuration
		java.util.logging.LogManager.getLogManager().reset(); // Clear JUL configuration
		java.util.logging.Logger.getLogger("global").setLevel(Level.FINEST); // Set JUL to FINEST (slf4j then filters messages)
		Properties log4jprops = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(new File(ApplicationProperties.instance().getInstallDirectory(), "conf/log4j.properties"));
			log4jprops.load(is);
		} catch (IOException e) {
			log.error("Error loading log4j configuration file", e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		PropertyConfigurator.configure(log4jprops);
	}
}
