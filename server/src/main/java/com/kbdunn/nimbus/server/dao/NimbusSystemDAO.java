package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hsqldb.types.Types;

import com.kbdunn.nimbus.common.model.nimbusphere.NimbusphereStatus;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public abstract class NimbusSystemDAO {

	private static Logger log = LogManager.getLogger(NimbusSystemDAO.class.getName());
	
	public static boolean getIsAutoScan() {	
		ensureSystemRecord();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT IS_AUTO_SCAN FROM NIMBUS.SYS;");
			rs = ps.executeQuery();
			if (!rs.next()) {
				log.warn("Auto Scan property not found in NIMBUS.SYS.");
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
		ensureSystemRecord();
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE NIMBUS.SYS "
					+ "SET IS_AUTO_SCAN = ?;");
			ps.setBoolean(1, isAutoScan);
			
			int i = ps.executeUpdate();
			if (i != 1) throw new SQLException("Update of NIMBUS.SYStem properties (SYS) failed! " + i + " records updated.");
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

	public static NimbusphereStatus getNimbusphereStatus() {	
		ensureSystemRecord();	
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT NIMBUSPHERE_TOKEN, IP_ADDRESS, NIMBUSPHERE_ADDRESS, IS_NIMBUSPHERE_VERIFIED, IS_NIMBUSPHERE_DELETED FROM NIMBUS.SYS;");
			rs = ps.executeQuery();
			if (!rs.next()) {
				log.warn("Nimbus.SYS record does not exist.");
				return null;
			}
			return new NimbusphereStatus(
					null,
					rs.getString(1),
					rs.getString(2),
					rs.getString(3),
					rs.getBoolean(4),
					rs.getBoolean(5)
				);
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
	
	public static void updateNimbusphereStatus(NimbusphereStatus status) {
		ensureSystemRecord();
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE NIMBUS.SYS SET "
					+ "NIMBUSPHERE_TOKEN = ?, "
					+ "NIMBUSPHERE_ADDRESS = ?, "
					+ "IP_ADDRESS = ?, "
					+ "IS_NIMBUSPHERE_VERIFIED = ?, "
					+ "IS_NIMBUSPHERE_DELETED = ?;");
			ps.setString(1, status.getToken());
			ps.setString(2, status.getAddress());
			ps.setString(3, status.getIp());
			if (status.isVerified() == null) {
				ps.setNull(4, Types.BOOLEAN);
			} else {
				ps.setBoolean(4, status.isVerified());
			}
			if (status.isDeleted() == null) {
				ps.setNull(5, Types.BOOLEAN);
			} else {
				ps.setBoolean(5, status.isDeleted());
			}
			
			int i = ps.executeUpdate();
			if (i != 1) throw new SQLException("Update of Nimbusphere properties (Nimbus.SYS) failed! " + i + " records updated.");
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
	
	private static void ensureSystemRecord() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement("SELECT ID FROM NIMBUS.SYS ORDER BY 1;");
			rs = ps.executeQuery();
			
			Long id = null;
			int n = 0;
			while (rs.next()) {
				id = id == null ? rs.getLong(1) : id;
				n++;
			}
			ps.close();
			ps = null;
			
			if (n == 0) {
				log.warn("The NIMBUS.SYS record does not exist. Creating it.");
				ps = con.prepareStatement("INSERT INTO NIMBUS.SYS (IS_AUTO_SCAN, CREATE_DATE, LAST_UPDATE_DATE)"
						+ "	VALUES (TRUE, SYSDATE, SYSDATE);");
			} else if (n > 1) {
				log.warn("Multiple NIMBUS.SYS records exist. Deleting extra rows.");
				ps = con.prepareStatement("DELETE FROM NIMBUS.SYS WHERE ID <> " + id + ";");
			}
			if (ps != null) ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e, e);
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
}
