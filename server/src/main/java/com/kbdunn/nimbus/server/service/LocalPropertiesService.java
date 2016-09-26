package com.kbdunn.nimbus.server.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.server.dao.NimbusSystemDAO;

public class LocalPropertiesService implements PropertiesService {
	
	private static final Logger log = LogManager.getLogger(LocalPropertiesService.class.getName());
	
	// This string can be used in the nimbus.properties file in place of the installation directory
	private static final String NIMBUS_HOME_PARM = Pattern.quote("${nimbus.home}");
	
	private static Boolean DEV_MODE; // Set if nimbus.home system property is unset
	private static Boolean DEMO_MODE; // Set based on existense of conf/demomode.ind
	
	private static String nimbusHome;
	private static String dbDriver;
	private static String dbConnectString;
	private static String dbUser;
	private static String dbPassword;
	private static Integer httpPort;
	private static Boolean sslEnabled;
	private static String keystorePassword;
	private static String keystorePath;
	private static String truststorePath;
	private static String truststorePassword;
	private static String keyManagerPassword;
	private static Integer httpsPort;
	private static Integer externalHttpsPort;
	
	private boolean autoScan;
	
	static {
		// Static properties
		nimbusHome = System.getProperty("nimbus.home");
		if (nimbusHome == null) {
			nimbusHome = System.getProperty("user.dir").replace("C:", "").replace("\\", "/");
			if (!nimbusHome.endsWith("nimbus")) {
				nimbusHome = nimbusHome.substring(0, nimbusHome.lastIndexOf("/"));
			}
			System.setProperty("nimbus.home", nimbusHome); // DEV Mode - set to Eclipse project home dir
			DEV_MODE = true;
			DEMO_MODE = false;
		} else {
			DEV_MODE = false;
			DEMO_MODE = new File(new File(nimbusHome), "conf/demomode.ind").exists();
		}
		dbUser = "SA";
		dbPassword = "nimbus";
		dbDriver = "org.hsqldb.jdbc.JDBCDriver";

		// Read the properties file only once
		Properties props = loadNimbusProperties();
		httpPort = getIntegerProperty(props, "nimbus.http.port");
		sslEnabled = getBooleanProperty(props, "nimbus.ssl.enabled");
		keystorePath = getStringProperty(props, "nimbus.ssl.keystore.path");
		keystorePassword = getStringProperty(props, "nimbus.ssl.keystore.password");
		truststorePath = getStringProperty(props, "nimbus.ssl.truststore.path");
		truststorePassword = getStringProperty(props, "nimbus.ssl.truststore.password");
		keyManagerPassword = getStringProperty(props, "nimbus.ssl.keymanager.password");
		httpsPort = getIntegerProperty(props, "nimbus.ssl.https.port");
		externalHttpsPort = getIntegerProperty(props, "nimbus.ssl.https.external.port");
		
		if (DEV_MODE) {
			dbConnectString = "jdbc:hsqldb:file:" + nimbusHome + "/server/src/main/resources/db/nimbusdb;ifexists=true;shutdown=true;hsqldb.write_delay=false;";
		} else {
			dbConnectString = "jdbc:hsqldb:file:" + nimbusHome + "/data/nimbusdb;ifexists=true;shutdown=true;hsqldb.write_delay=false;";
		}
	}
	
	private static Properties loadNimbusProperties() {
		Properties props = new Properties();
		InputStream is = null;
		try {
			if (DEV_MODE) {
				is = new FileInputStream(new File(System.getProperty("nimbus.home") + "/server/src/main/resources/nimbus.properties"));
			} else {
				is = new FileInputStream(new File(System.getProperty("nimbus.home") + "/conf/nimbus.properties"));
			}
			props.load(is);
		} catch (IOException e) {
			log.error(e, e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		return props;
	}
	
	private static String getStringProperty(Properties props, String key) {
		if (props.getProperty(key) == null) return null;
		return props.getProperty(key).replaceAll(NIMBUS_HOME_PARM, System.getProperty("nimbus.home"));
	}

	private static Boolean getBooleanProperty(Properties props, String key) {
		if (props.getProperty(key) == null) return null;
		return Boolean.parseBoolean(props.getProperty(key));
	}
	
	private static Integer getIntegerProperty(Properties props, String key) {
		if (props.getProperty(key) == null) return null;
		try {
			return Integer.parseInt(props.getProperty(key));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public boolean isDevMode() {
		return DEV_MODE;
	}
	
	@Override
	public boolean isDemoMode() {
		return DEMO_MODE;
	}

	@Override
	public String getDemoUsername() {
		return "DemoUser";
	}
	
	@Override
	public String getNimbusHome() {
		return nimbusHome;
	}

	@Override
	public String getDbDriver() {
		return dbDriver;
	}

	@Override
	public String getDbConnectString() {
		return dbConnectString;
	}

	@Override
	public String getDbUser() {
		return dbUser;
	}

	@Override
	public String getDbPassword() {
		return dbPassword;
	}

	@Override
	public Integer getHttpPort() {
		return httpPort;
	}

	@Override
	public Boolean isSslEnabled() {
		return sslEnabled;
	}

	@Override
	public String getKeystorePassword() {
		return keystorePassword;
	}
	
	@Override
	public String getKeystorePath() {
		return keystorePath;
	}

	@Override
	public String getTruststorePath() {
		return truststorePath;
	}

	@Override
	public String getTruststorePassword() {
		return truststorePassword;
	}

	@Override
	public String getKeyManagerPassword() {
		return keyManagerPassword;
	}

	@Override
	public Integer getHttpsPort() {
		return httpsPort;
	}

	@Override
	public Integer getExternalHttpsPort() {
		return externalHttpsPort;
	}
	
	@Override
	public String getNimbusVersion() {
		if (this.isDevMode()) return "0.6.3.1161";
		return LocalPropertiesService.class.getPackage().getImplementationVersion();
	}
	
	@Override
	public NimbusphereStatus getNimbusphereStatus() {
		return NimbusSystemDAO.getNimbusphereStatus();
	}
	
	/* MODIFYABLE PROPERTIES */
	
	@Override
	public boolean isAutoScan() {
		autoScan = NimbusSystemDAO.getIsAutoScan();
		return autoScan;
	}
	
	@Override
	public void setAutoScan(boolean autoScan) {
		this.autoScan = autoScan;
		save();
	}
	
	private void save() {
		NimbusSystemDAO.update(autoScan);
	}
}
