package com.kbdunn.nimbus.server.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.server.NimbusContext;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class HikariConnectionPool {

	public static final String SCHEMA_NAME = "NIMBUS";
	
	private static HikariConnectionPool instance;
	
	private final HikariDataSource ds;
	
	private HikariConnectionPool() {
		final HikariConfig config = new HikariConfig();
		final PropertiesService props = NimbusContext.instance().getPropertiesService();
		config.setJdbcUrl(props.getDbConnectString());
		config.setUsername(props.getDbUser());
		config.setPassword(props.getDbPassword());
		config.setMaximumPoolSize(8);
		config.setConnectionInitSql("SET SCHEMA " + SCHEMA_NAME);
		
		ds = new HikariDataSource(config);
	}
	
	public synchronized static void init() {
		if (instance == null) {
			instance = new HikariConnectionPool();
		}
	}
	
	public synchronized static void destroy() {
		if (instance != null) {
			instance.ds.close();
		}
		instance = null;
	}
	
	public static HikariConnectionPool instance() {
		init();
		return instance;
	}
	
	public static Connection getConnection() throws SQLException {
		init();
		return instance.ds.getConnection();
	}
}
