package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.OAuthCredential;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public class OAuthDAO {
	
	private static final Logger log = LogManager.getLogger(OAuthDAO.class);

	public static OAuthCredential getCredential(long userId, String serviceName) {
		log.trace("OAuthCredential " + serviceName + " for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * "
					+ "FROM USER_OAUTH WHERE USER_ID = ? AND SERVICE_NAME = ?;");
			ps.setLong(1, userId);
			ps.setString(2, serviceName);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("OAuthCredential " + serviceName + " for user ID " + userId + " was not found");
				return null;
			}
			return toOAuthCredential(rs);
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

	public static List<OAuthCredential> getCredentials(long userId) {
		log.trace("Getting all OAuth credentials for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * "
					+ "FROM USER_OAUTH WHERE USER_ID = ?;");
			ps.setLong(1, userId);
			rs = ps.executeQuery();
			return toOAuthCredentialList(rs);
			
		} catch (SQLException e) {
			log.error(e, e);
			return Collections.emptyList();
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

	public static boolean insert(OAuthCredential credential) {
		log.trace("Creating OAuth credential "+ credential);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO USER_OAUTH "
					+ "(USER_ID, SERVICE_NAME, ACCESS_TOKEN, REFRESH_TOKEN, EXPIRES_IN, LAST_REFRESH, TOKEN_TYPE, "
					+ "OAUTH1_PUBLIC_TOKEN, SCOPE, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			int idx = 0;
			ps.setLong(++idx, credential.getUserId());
			ps.setString(++idx, credential.getServiceName().toString());
			ps.setString(++idx, credential.getAccessToken());
			ps.setString(++idx, credential.getRefreshToken());
			ps.setInt(++idx, credential.getExpiresIn());
			ps.setLong(++idx, credential.getLastRefresh());
			ps.setString(++idx, credential.getTokenType());
			ps.setString(++idx, credential.getOAuth1PublicToken());
			ps.setString(++idx, credential.getScope());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of USER_OAUTH record failed " + credential);
			return true;
			
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}

	public static boolean update(OAuthCredential credential) {
		log.trace("Updating " + credential);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE USER_OAUTH "
					+ "SET USER_ID=?, "
					+ "SERVICE_NAME=?, "
					+ "ACCESS_TOKEN=?, "
					+ "REFRESH_TOKEN=?, "
					+ "EXPIRES_IN=?, "
					+ "LAST_REFRESH=?, "
					+ "TOKEN_TYPE=?, "
					+ "OAUTH1_PUBLIC_TOKEN=?, "
					+ "SCOPE=?, "
					+ "LAST_UPDATE_DATE=SYSDATE "
					+ "WHERE ID=?;");
			int idx = 0;
			ps.setLong(++idx, credential.getUserId());
			ps.setString(++idx, credential.getServiceName().toString());
			ps.setString(++idx, credential.getAccessToken());
			ps.setString(++idx, credential.getRefreshToken());
			ps.setInt(++idx, credential.getExpiresIn());
			ps.setLong(++idx, credential.getLastRefresh());
			ps.setString(++idx, credential.getTokenType());
			ps.setString(++idx, credential.getOAuth1PublicToken());
			ps.setString(++idx, credential.getScope());
			ps.setLong(++idx, credential.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of USER_OAUTH record failed " + credential);
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static boolean delete(long credentialId) {
		log.trace("Deleting OAuth credential ID " + credentialId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM USER_OAUTH WHERE ID = ?;");
			
			ps.setLong(1, credentialId);
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of USER_OAUTH record failed");
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static boolean delete(long userId, String serviceType) {
		log.trace("Deleting OAuth service type " + serviceType + " for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM USER_OAUTH WHERE USER_ID = ? AND SERVICE_NAME = ?;");
			
			ps.setLong(1, userId);
			ps.setString(2, serviceType);
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	static List<OAuthCredential> toOAuthCredentialList(ResultSet rs) {
		List<OAuthCredential> result = new ArrayList<>();
		try {
			while (rs.next()) 
				result.add(toOAuthCredential(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static OAuthCredential toOAuthCredential(ResultSet row) {
		try {
			Long id = row.getLong("ID");
			Long userId = row.getLong("USER_ID");
			String serviceName = row.getString("SERVICE_NAME");
			OAuthAPIService.Type service = serviceName == null ? null : OAuthAPIService.Type.valueOf(serviceName);
			String accessToken = row.getString("ACCESS_TOKEN");
			String refreshToken = row.getString("REFRESH_TOKEN");
			Integer expiresIn = row.getInt("EXPIRES_IN");
			Long lastRefresh = row.getLong("LAST_REFRESH");
			String tokenType = row.getString("TOKEN_TYPE");
			String oAuth1PublicToken = row.getString("OAUTH1_PUBLIC_TOKEN");
			String scope = row.getString("SCOPE");
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new OAuthCredential(id, userId, service, accessToken, refreshToken, expiresIn, 
					lastRefresh, tokenType, oAuth1PublicToken, scope, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
}
