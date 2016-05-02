package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public abstract class NimbusSystemDAO {

	private static Logger log = LogManager.getLogger(NimbusSystemDAO.class.getName());
	
	public static String getVersion() {		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT VERSION FROM SYS;");
			rs = ps.executeQuery();
			if (!rs.next()) {
				log.warn("Version property not found in SYS.");
				return null;
			}
			return rs.getString(1);
		} catch (SQLException e) {
			log.error(e, e);
			return null;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
				if (rs != null) rs.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static boolean getIsAutoScan() {		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT IS_AUTO_SCAN FROM SYS;");
			rs = ps.executeQuery();
			if (!rs.next()) {
				log.warn("Auto Scan property not found in SYS.");
				return false;
			}
			return rs.getBoolean(1);
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
				if (rs != null) rs.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static void update(boolean isAutoScan) {		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE SYS "
					+ "SET IS_AUTO_SCAN = ?;");
			ps.setBoolean(1, isAutoScan);
			
			int i = ps.executeUpdate();
			if (i != 1) throw new SQLException("Update of system properties (SYS) failed! " + i + " records updated.");
		} catch (SQLException e) {
			log.error(e, e);
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
}
