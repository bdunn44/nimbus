package com.kbdunn.nimbus.desktop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationProperties {

	private static final Logger log = LoggerFactory.getLogger(ApplicationProperties.class);
	public static final String PROP_INSTALL_DIR = "com.kbdunn.nimbus.sync.installdir";
	public static final String PROP_SYNC_DIR = "com.kbdunn.nimbus.sync.syncdir";
	
	private static ApplicationProperties instance;
	
	private Properties properties;
	
	private ApplicationProperties() {  }
	
	public static ApplicationProperties instance() {
		if (instance == null) {
			instance = new ApplicationProperties();
		}
		return instance;
	}
	
	public void initialize() {
		String installDir = System.getProperty(PROP_INSTALL_DIR);
		if (installDir == null) {
			// We're in DEV mode, but let's be sure
			installDir = System.getProperty("user.dir") + "/src/main/resources/dev";
			System.out.println(installDir);
			if (!new File(installDir).isDirectory())
				throw new IllegalStateException("The " + PROP_INSTALL_DIR + " property is not set");
		}
		properties = new Properties();
		properties.setProperty(PROP_INSTALL_DIR, installDir);
		try (final InputStream in = new FileInputStream(new File(getInstallDirectory(), "conf/nimbus-sync.properties"))) {
			properties.load(in);
		} catch (IOException e) {
			log.error("Error reading application properties." , e);
		}
	}
	
	public String getInstallDirectoryPath() {
		return properties.getProperty(PROP_INSTALL_DIR);
	}
	
	public File getInstallDirectory() {
		return new File(getInstallDirectoryPath());
	}
	
	public String getSyncDirectoryPath() {
		String dir = Pattern.quote(properties.getProperty(PROP_SYNC_DIR)); // Handle backslashes
		if (dir == null || dir.isEmpty()) {
			dir = System.getProperty("user.home") + File.separator + "Nimbus Sync";
			properties.setProperty(PROP_SYNC_DIR, dir);
		}
		return dir;
	}
	
	public File getSyncDirectory() {
		return new File(getSyncDirectoryPath());
	}
}
