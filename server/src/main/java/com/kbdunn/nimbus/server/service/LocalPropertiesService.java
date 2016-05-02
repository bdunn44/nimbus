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
	
	private boolean autoScan;
	
	static {
		// Static properties
		nimbusHome = System.getProperty("nimbus.home");
		if (nimbusHome == null) {
			nimbusHome = System.getProperty("user.dir");
			if (!nimbusHome.endsWith("nimbus")) {
				System.out.println(nimbusHome);
				nimbusHome = nimbusHome.substring(0, nimbusHome.lastIndexOf(File.separator));
			}
			System.setProperty("nimbus.home", nimbusHome); // DEV Mode - set to Eclipse project home dir
			DEV_MODE = true;
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

		if (DEV_MODE) {
			dbConnectString = "jdbc:hsqldb:file:" + nimbusHome + "/server/src/main/resources/db/nimbusdb;ifexists=true;shutdown=true;hsqldb.write_delay=false;";
		//} else if (DEMO_MODE) {
		//	dbConnectString = "jdbc:hsqldb:file:/nimbus/db/nimbusdb;ifexists=true;shutdown=true;hsqldb.write_delay=false;";
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

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#isDevMode()
	 */
	@Override
	public boolean isDevMode() {
		return DEV_MODE;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#isDemoMode()
	 */
	@Override
	public boolean isDemoMode() {
		return DEMO_MODE;
	}

	@Override
	public String getDemoUsername() {
		return "DemoUser";
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getNimbusHome()
	 */
	@Override
	public String getNimbusHome() {
		return nimbusHome;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getDbDriver()
	 */
	@Override
	public String getDbDriver() {
		return dbDriver;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getDbConnectString()
	 */
	@Override
	public String getDbConnectString() {
		return dbConnectString;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getDbUser()
	 */
	@Override
	public String getDbUser() {
		return dbUser;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getDbPassword()
	 */
	@Override
	public String getDbPassword() {
		return dbPassword;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getHttpPort()
	 */
	@Override
	public Integer getHttpPort() {
		return httpPort;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#isSslEnabled()
	 */
	@Override
	public Boolean isSslEnabled() {
		return sslEnabled;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getKeystorePassword()
	 */
	@Override
	public String getKeystorePassword() {
		return keystorePassword;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getKeystorePath()
	 */
	@Override
	public String getKeystorePath() {
		return keystorePath;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getTruststorePath()
	 */
	@Override
	public String getTruststorePath() {
		return truststorePath;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getTruststorePassword()
	 */
	@Override
	public String getTruststorePassword() {
		return truststorePassword;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getKeyManagerPassword()
	 */
	@Override
	public String getKeyManagerPassword() {
		return keyManagerPassword;
	}

	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#getHttpsPort()
	 */
	@Override
	public Integer getHttpsPort() {
		return httpsPort;
	}
	
	@Override
	public String getNimbusVersion() {
		return NimbusSystemDAO.getVersion();
	}
	
	/* MODIFYABLE PROPERTIES */
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#isAutoScan()
	 */
	@Override
	public boolean isAutoScan() {
		autoScan = NimbusSystemDAO.getIsAutoScan();
		return autoScan;
	}
	
	/* (non-Javadoc)
	 * @see com.kbdunn.nimbus.server.util.PropertiesService#setAutoScan(boolean)
	 */
	@Override
	public void setAutoScan(boolean autoScan) {
		this.autoScan = autoScan;
		save();
	}
	
	private void save() {
		NimbusSystemDAO.update(autoScan);
	}
}
