package com.kbdunn.nimbus.server.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.jdbc.JdbcHelper;

public class DatabaseCleaner {

	private static final Logger log = LogManager.getLogger(DatabaseCleaner.class.getName());
	
	public static void main(String[] args) {
		new DatabaseCleaner().rebuild();
	}
	
	public DatabaseCleaner rebuild() {
		Connection con = null;
		try {
			con = JdbcHelper.createConnection();//HikariConnectionPool.getConnection();
			if (getClass().getResource("/db/nimbusdb-ddl.sql") != null) {
				// If DDL exists rebuild schema
				log.info("Rebuilding " + HikariConnectionPool.SCHEMA_NAME + " schema");
				final String ddl = new String(Files.readAllBytes(Paths.get(getClass().getResource("/db/nimbusdb-ddl.sql").toURI())));
				con.createStatement().execute("DROP SCHEMA NIMBUS IF EXISTS CASCADE");
				for (String statement : ddl.split(";")) {
					if (statement.trim().isEmpty()) continue;
					con.createStatement().execute(statement.trim());
				}
				con.createStatement().execute("CHECKPOINT DEFRAG");
				con.createStatement().execute("DISCONNECT");
			} else {
				// Otherwise clean data
				clean(con);
			}
		} catch (Exception e) {
			if (con == null || !clean(con))
				log.error(e, e);
		} finally {
			if (con != null) {
				try {
					con.close();
				} catch (SQLException e) {
					// Ignore
				}
			}
		}
		return this;
	}
	
	private boolean clean(Connection con) {
		log.info("Cleaning " + HikariConnectionPool.SCHEMA_NAME + " schema");
		try {
			con.createStatement().execute("TRUNCATE SCHEMA " + HikariConnectionPool.SCHEMA_NAME + " RESTART IDENTITY AND COMMIT NO CHECK");
			con.createStatement().execute("CHECKPOINT DEFRAG");
			con.createStatement().execute("DISCONNECT");
			return true;
		} catch (Exception e) {
			log.error(e, e);
			return false;
		}
	}
}
