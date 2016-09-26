package com.kbdunn.nimbus.common.server;

import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;

public interface PropertiesService {

	boolean isDevMode();
	
	boolean isDemoMode();
	
	String getDemoUsername();
	
	String getNimbusHome();

	String getDbDriver();

	String getDbConnectString();

	String getDbUser();

	String getDbPassword();

	Integer getHttpPort();

	Boolean isSslEnabled();

	String getKeystorePassword();

	String getKeystorePath();

	String getTruststorePath();

	String getTruststorePassword();

	String getKeyManagerPassword();

	Integer getHttpsPort();

	Integer getExternalHttpsPort();

	String getNimbusVersion();
	
	boolean isAutoScan();

	void setAutoScan(boolean autoScan);

	NimbusphereStatus getNimbusphereStatus();
}