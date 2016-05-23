package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hsqldb.types.Types;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.SMTPSettings;
import com.kbdunn.nimbus.common.security.OAuthAPIService;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public abstract class NimbusUserDAO {
	
	private static final Logger log = LogManager.getLogger(NimbusUserDAO.class.getName());
	
	public static Integer getUserCount() {

		Connection con = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			rs = con.createStatement().executeQuery("SELECT COUNT(1) FROM USER");
			if (!rs.next()) throw new SQLException("There was an error getting the user count");
			return rs.getInt(1);
		} catch (SQLException e) {
			log.error(e, e);
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (con != null) con.close();
				if (rs != null) rs.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	// Retrieves a user by email or name
	public static NimbusUser getByDomainKey(String domainKey) {
		log.trace("Getting user " + domainKey);
		NimbusUser user = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			// Query the database for the username. Grabs matches for email or name
			ps = con.prepareStatement(
					"SELECT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u " +
					"WHERE UPPER(u.NAME)=? " +
					"OR UPPER(u.EMAIL)=?");
			ps.setString(1, domainKey.toUpperCase());
			ps.setString(2, domainKey.toUpperCase());
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("User not found!");
				return null;
			}
			return toNimbusUser(rs);
			
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
		
		return user;
	}
	
	// Get user by API Token
	public static NimbusUser getByApiToken(String apiToken) {
		log.trace("Getting user by API token " + apiToken);
		NimbusUser user = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			// Query the database for the username. Grabs matches for email or name
			ps = con.prepareStatement(
					"SELECT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u " +
					"WHERE API_TOKEN=?");
			ps.setString(1, apiToken);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("User not found!");
				return null;
			}
			return toNimbusUser(rs);
			
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
		
		return user;
	}
	
	// Retrieves a user by ID
	public static NimbusUser getById(Long id) {
		log.trace("Getting user with ID " + id);
		NimbusUser user = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			// Query the database for the username. Grabs matches for email or name
			ps = con.prepareStatement(
					"SELECT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u " +
					"WHERE u.ID = ?");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("User with ID " + id + " not found!");
				return null;
			}
			return toNimbusUser(rs);
			
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
		
		return user;
	}
	
	// Retrieves a user by ID
	public static NimbusUser getByUserDriveId(Long id) {
		log.debug("Getting user for USER_STORAGE ID " + id);
		NimbusUser user = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			// Query the database for the username. Grabs matches for email or name
			ps = con.prepareStatement(
					"SELECT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u JOIN USER_STORAGE ud ON u.ID = USER_STORAGE.USER_ID " +
					"WHERE ud.ID = ?");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("User for USER_STORAGE ID " + id + " not found!");
				return null;
			}
			return toNimbusUser(rs);
			
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
		
		return user;
	}
	
	public static List<NimbusUser> getAll() {
		log.debug("Getting all users");
		List<NimbusUser> users = null;
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u ORDER BY u.CREATE_DATE ASC;"
					);
			rs = ps.executeQuery();
			
			return toNimbusUserList(rs);
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
		
		return users;
	}
	
	// Test if an email exists in the database, optionally ignoring a single user ID (for use when changing username)
	public static boolean isDuplicateEmail(String email, Long ignoreId) {
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			String q = "SELECT 0 FROM USER "
					+ "WHERE UPPER(EMAIL) = ?";
			q += ignoreId != null  ? " AND ID <> ?;" : ";";
			
			ps = con.prepareStatement(q);
			ps.setString(1, email.toUpperCase());
			if (ignoreId != null) ps.setLong(2, ignoreId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return true;
			}
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
		return false;
	}
	
	// Test if a user name exists in the database, optionally ignoring a single user ID
	public static boolean isDuplicateName(String name, Long ignoreId) {
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			String q = "SELECT 0 FROM USER "
					+ "WHERE UPPER(NAME) = ?";
			q += ignoreId != null ? " AND ID <> ?;" : ";";
			ps = con.prepareStatement(q);
			ps.setString(1, name.toUpperCase());
			if (ignoreId != null) ps.setLong(2, ignoreId);
			rs = ps.executeQuery();
			if (rs.next()) {
				return true;
			}
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
		return false;
	}
	
	// Create a user. Returns newly created user ID
	public static boolean insert(NimbusUser user) {
		log.debug("Creating user in database " + user);
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			// Insert User
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO USER "
					+ "(NAME, EMAIL, PW_DIGEST, HAS_TEMP_PW, IS_ADMIN, HMAC_KEY, API_TOKEN, OAUTH_EMAIL_SERVICE_NAME, CREATE_DATE, LAST_UPDATE_DATE)"
					+ "VALUES"
					+ "(?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			ps.setString(1, user.getName());
			ps.setString(2, user.getEmail());
			ps.setString(3, user.getPasswordDigest());
			ps.setBoolean(4, user.isPasswordTemporary());
			ps.setBoolean(5, user.isAdministrator());
			ps.setString(6, user.getHmacKey());
			ps.setString(7, user.getApiToken());
			if (user.getEmailServiceName() != null) {
				ps.setString(8, user.getEmailServiceName().toString());
			} else {
				ps.setNull(8, Types.VARCHAR);
			}
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of USER record failed");
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
		return true;
	}
	
	// Update a user
	public static boolean update(NimbusUser user) {
		log.debug("Updating user " + user);
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE USER SET "
					+ "NAME = ?, "
					+ "EMAIL = ?, "
					+ "PW_DIGEST = ?, "
					+ "HAS_TEMP_PW = ?, "
					+ "IS_ADMIN = ?, "
					+ "HMAC_KEY = ?, "
					+ "API_TOKEN = ?, "
					+ "OAUTH_EMAIL_SERVICE_NAME = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?");
			ps.setString(1, user.getName());
			ps.setString(2, user.getEmail());
			ps.setString(3, user.getPasswordDigest());
			ps.setBoolean(4, user.isPasswordTemporary());
			ps.setBoolean(5, user.isAdministrator());
			ps.setString(6, user.getHmacKey());
			ps.setString(7, user.getApiToken());
			if (user.getEmailServiceName() != null) {
				ps.setString(8, user.getEmailServiceName().toString());
			} else {
				ps.setNull(8, Types.VARCHAR);
			}
			ps.setLong(9, user.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of USER record failed");
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

		return true;
	}
	
	// Delete a user
	public static boolean delete(Long userId) {
		log.debug("Deleting user with ID " + userId);
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement("DELETE FROM USER WHERE ID = ?;");
			ps.setLong(1, userId);
			if (ps.executeUpdate() != 1) throw new SQLException("Delete of USER record failed");
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
		return true;
	}

	public static SMTPSettings getSmtpSettings(NimbusUser user) {
		log.debug("Getting SMTP settings for user " + user);
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT SMTP_SERVER, SMTP_PORT, SMTP_SSL_ENABLED, SMTP_SSL_PORT, EMAIL, SMTP_PW, ID "
					+ "FROM USER WHERE ID = ?;");
			ps.setLong(1, user.getId());
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("There is no user with ID " + user.getId());
			SMTPSettings smtp = new SMTPSettings(rs.getLong(7));
			smtp.setSmtpServer(rs.getString(1));
			smtp.setSmtpPort(rs.getString(2));
			smtp.setSslEnabled(rs.getBoolean(3));
			smtp.setSslPort(rs.getString(4));
			smtp.setUsername(rs.getString(5));
			smtp.setPassword(rs.getString(6));
			if (smtp.noAttributesSet()) return null;
			return smtp;
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
		return null;
	}

	public static boolean updateSmtpSettings(SMTPSettings smtpSettings) {
		log.debug("Updating SMTP settings for user ID " + smtpSettings.getUserId());
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE USER SET "
					+ "SMTP_SERVER = ?, "
					+ "SMTP_PORT = ?, "
					+ "SMTP_SSL_ENABLED = ?, "
					+ "SMTP_SSL_PORT = ?, "
					+ "EMAIL = ?, "
					+ "SMTP_PW = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			ps.setString(1, smtpSettings.getSmtpServer());
			ps.setString(2, smtpSettings.getSmtpPort());
			ps.setBoolean(3, smtpSettings.isSslEnabled());
			ps.setString(4, smtpSettings.getSslPort());
			ps.setString(5, smtpSettings.getUsername());
			ps.setString(6, smtpSettings.getPassword());
			ps.setLong(7, smtpSettings.getUserId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("The SMTP settings were not saved");
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
		return true;
	}
	
	static List<NimbusUser> toNimbusUserList(ResultSet rs) {
		List<NimbusUser> result = new LinkedList<>();
		try {
			while (rs.next()) 
				result.add(toNimbusUser(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static NimbusUser toNimbusUser(ResultSet row) {
		try {
			Long id = row.getLong("ID");
			String name = row.getString("NAME");
			String email = row.getString("EMAIL");
			String passwordDigest = row.getString("PW_DIGEST");
			String apiToken = row.getString("API_TOKEN");
			String hmacKey = row.getString("HMAC_KEY");
			Boolean isPasswordTemporary = row.getBoolean("HAS_TEMP_PW");
			Boolean isAdministrator = row.getBoolean("IS_ADMIN");
			Boolean isOwner = row.getBoolean("IS_OWNER");
			String oAuthEmailServiceName = row.getString("OAUTH_EMAIL_SERVICE_NAME");
			OAuthAPIService.Type oAuthEmailService = oAuthEmailServiceName == null ? null : OAuthAPIService.Type.valueOf(oAuthEmailServiceName);
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new NimbusUser(id, name, email, passwordDigest, apiToken, hmacKey, 
					isPasswordTemporary, isAdministrator, isOwner, oAuthEmailService, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
}
