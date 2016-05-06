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
import org.hsqldb.types.Types;

import com.kbdunn.nimbus.common.model.FilesystemLocation;
import com.kbdunn.nimbus.common.model.HardDrive;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.common.model.StorageDevice;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.jdbc.JdbcHelper;

public abstract class StorageDAO {
	
	private static Logger log = LogManager.getLogger(StorageDAO.class.getName());
	
	// Retrieves the drive by ID
	public static StorageDevice getById(long id) {
		log.trace("getById() called for Drive ID " + id);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE " +
					"WHERE ID=?;");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("Drive " + id + " not found.");
				return null;
			}
			log.trace("Drive found");
			return toStorageDevice(rs);
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
	
	// Retrieves the drive by path
	public static StorageDevice getByPath(String path) {
		log.trace("getByPath() called for path " + path);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE " +
					"WHERE PATH = ?;");
			ps.setString(1, path);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("Drive " + path + " not found.");
				return null;
			}
			log.trace("Drive found");
			return toStorageDevice(rs);
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
	
	// Retrieves the drive by path
	public static StorageDevice getByDevicePath(String devicePath) {
		log.trace("getByDevicePath() called for path " + devicePath);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE " +
					"WHERE DEVICE_PATH = ?;");
			ps.setString(1, devicePath);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("Drive " + devicePath + " not found.");
				return null;
			}
			log.trace("Drive found");
			return toStorageDevice(rs);
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
	
	public static HardDrive getHardDriveByUuid(String uuid) {
		log.trace("getByUuid() called for UUID " + uuid);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE " +
					"WHERE UUID=?;");
			ps.setString(1, uuid);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.debug("Drive " + uuid + " not found");
				return null;
			}
			log.trace("Drive found");
			return (HardDrive) toStorageDevice(rs);
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
	
	public static long getUsedBytes(long driveId) {
		log.trace("getUsedBytes() called for id " + driveId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT SUM(f.SIZE) "
					+ "FROM STORAGE s "
					+ "JOIN USER_STORAGE us ON us.STORAGE_ID = s.ID "
					+ "JOIN FILE f ON f.USER_STORAGE_ID = us.ID " +
					"WHERE s.ID = ?;");
			ps.setLong(1, driveId);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				return 0;
			}
			log.trace("Drive found");
			return rs.getLong(1);
		} catch (SQLException e) {
			log.error(e, e);
			return 0;
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
	
	public static StorageDevice getUserDrive(long driveId, long userId) {
		log.trace("getUserDrive() called for drive ID " + driveId + " and user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.* FROM USER_STORAGE ud " +
					"JOIN USER u ON u.ID = ud.USER_ID " +
					"JOIN STORAGE d ON d.ID = ud.STORAGE_ID " +
					"WHERE u.ID=? " +
					"AND d.ID = ?;");
			ps.setLong(1, userId);
			ps.setLong(2, driveId);
			rs = ps.executeQuery();
			
			if (!rs.next()) return null;
			return toStorageDevice(rs);
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
	
	public static List<StorageDevice> getUserDrives(long userId) {
		log.debug("getAllUserDrives() called for User ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.* FROM STORAGE d " +
					"JOIN USER_STORAGE ud ON ud.STORAGE_ID = d.ID " +
					"WHERE ud.USER_ID = ?; ");
			ps.setLong(1, userId);
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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
	
	/*public static List<StorageDevice> getAvailableUserDrives(long userId) {
		log.trace("getAvailableUserDrives() called for User ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.*, ud.IS_ACTIVE FROM STORAGE d " +
					"JOIN USER_STORAGE ud ON ud.STORAGE_ID = d.ID " +
					"WHERE ud.USER_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?)"
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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
	}*/
	
	public static List<StorageDevice> getStorageDevicesAssignedToUser(long userId) {
		log.trace("getActiveUserDrives() called for User ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.* FROM STORAGE d " +
					"JOIN USER_STORAGE ud ON ud.STORAGE_ID = d.ID " +
					"WHERE ud.USER_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?);");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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
	
	public static List<NimbusUser> getUsersAssignedToDevice(long driveId) {
		log.trace("getUsersAssignedToDrive() called for Drive ID " + driveId);
		
		List<NimbusUser> result = new ArrayList<NimbusUser>();
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT u.*, " +
					// Check if owner
					"(CASE u.CREATE_DATE WHEN (SELECT MIN(CREATE_DATE) FROM USER) THEN TRUE ELSE FALSE END) IS_OWNER " +
					"FROM USER u " +
					"JOIN USER_STORAGE ud ON ud.USER_ID = u.ID " +
					"WHERE ud.STORAGE_ID = ?; ");
			ps.setLong(1, driveId);
			rs = ps.executeQuery();
			
			while (rs.next()) {
				NimbusUser user = new NimbusUser();
				user.setId(rs.getLong("ID"));
				user.setName(rs.getNString("NAME"));
				user.setEmail(rs.getNString("EMAIL"));
				user.setPasswordDigest(rs.getNString("PW_DIGEST"));
				user.setPasswordTemporary(rs.getBoolean("HAS_TEMP_PW"));
				user.setOwner(rs.getBoolean("IS_OWNER"));
				user.setAdministrator(rs.getBoolean("IS_ADMIN"));
				user.setCreated(rs.getTimestamp("CREATE_DATE"));
				user.setUpdated(rs.getTimestamp("LAST_UPDATE_DATE"));
				result.add(user);
			}
			return result;
		} catch (SQLException e) {
			log.error(e, e);
			return result;
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
	
	/*public static List<NimbusUser> getAssignedUsers(Drive drive) {
		List<NimbusUser> users = new ArrayList<NimbusUser>();
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HSQLConnectionPool.getCurrentConnection();
			ps = con.prepareStatement(
					"SELECT u.* FROM USER u " +
					"JOIN USER_STORAGE ud ON u.ID = ud.USER_ID " +
					"WHERE ud.STORAGE_ID=?;");
			ps.setLong(1, drive.getId());
			rs = ps.executeQuery();
			
			while (rs.next()) {
				NimbusUser user = new NimbusUser();
				user.setId(rs.getLong("ID"));
				user.setName(rs.getNString("NAME"));
				user.setEmail(rs.getNString("EMAIL"));
				user.setPasswordDigest(rs.getNString("PW_DIGEST"));
				user.setPasswordTemporary(rs.getBoolean("HAS_TEMP_PW"));
				user.setAdministrator(rs.getBoolean("IS_ADMIN"));
				user.setCreateDate(rs.getTimestamp("CREATE_DATE"));
				user.setLastUpdateDate(rs.getTimestamp("LAST_UPDATE_DATE"));
				users.add(user);
			}
		
		} catch (SQLException e) {
			log.error(e, e);
		} finally {
			try {
				HSQLConnectionPool.releaseCurrentConnection(con);
				if (ps != null) ps.close();
				if (rs != null) rs.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
		log.trace("getAssignedUsers() returned " + users.size() + " users.");
		return users;
	}*/
	
	public static List<NimbusFile> getFiles(long driveId) {
		log.trace("getFiles() called for drive ID " + driveId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT f.* FROM FILE f " +
					"JOIN USER_STORAGE ud ON ud.ID = f.USER_STORAGE_ID "
					+ "WHERE ud.STORAGE_ID=?;");
			ps.setLong(1, driveId);
			rs = ps.executeQuery();
			
			return NimbusFileDAO.toNimbusFileList(rs);
		
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
	
	public static List<StorageDevice> getAll() {
		log.trace("getAll() called");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE;");
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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

	public static List<HardDrive> getConnectedHardDrives() {
		log.trace("getConnected() called");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE WHERE TYPE <> ? AND IS_CONNECTED = TRUE;");
			ps.setString(1, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			
			List<HardDrive> result = new ArrayList<HardDrive>();
			for (StorageDevice sd : toStorageDeviceList(rs)) {
				result.add((HardDrive) sd);
			}
			return result;
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

	public static List<HardDrive> getAvailable() {
		log.trace("getAvailable() called");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM STORAGE WHERE TYPE = ? OR (IS_CONNECTED = TRUE AND IS_MOUNTED = TRUE);");
			ps.setString(1, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			
			List<HardDrive> result = new ArrayList<HardDrive>();
			for (StorageDevice sd : toStorageDeviceList(rs)) {
				result.add((HardDrive) sd);
			}
			return result;
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
	
	public static List<StorageDevice> getAssigned() {
		log.trace("getAssigned() called");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.* FROM STORAGE d JOIN USER_STORAGE ud ON d.ID = ud.STORAGE_ID;");
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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
	
	/*public static List<StorageDevice> getActiveUserStorageDevices() {
		log.trace("getActiveAssigned() called");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT d.* FROM STORAGE d JOIN USER_STORAGE ud ON d.ID = ud.STORAGE_ID "
					+ "WHERE ud.IS_ACTIVE;");
			rs = ps.executeQuery();
			
			return toStorageDeviceList(rs);
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
	}*/
	
	public static boolean insert(StorageDevice d) {
		log.debug("Inserting " + d);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO STORAGE (NAME, PATH, IS_RECONCILED, TYPE, IS_AUTONOMOUS, USED, LABEL, UUID, "
					+ "IS_CONNECTED, IS_MOUNTED, SIZE, DEVICE_PATH, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			int i = 0;
			ps.setString(++i, d.getName());
			ps.setString(++i, d.getPath());
			ps.setBoolean(++i, d.isReconciled());
			ps.setString(++i, d.getType());
			ps.setBoolean(++i, d.isAutonomous());
			if (d instanceof HardDrive) {
				HardDrive hd = (HardDrive) d;
				if (hd.getUsed() != null) ps.setLong(++i, hd.getUsed()); else ps.setLong(++i, 0);
				ps.setString(++i, hd.getLabel());
				ps.setString(++i, hd.getUuid());
				ps.setBoolean(++i, hd.isConnected());
				ps.setBoolean(++i, hd.isMounted());
				if (hd.getSize() != null) ps.setLong(++i, hd.getSize()); else ps.setLong(++i, 0);
				ps.setString(++i, hd.getDevicePath());
			} else {
				ps.setNull(++i, Types.INTEGER);
				ps.setNull(++i, Types.VARCHAR);
				ps.setNull(++i, Types.VARCHAR);
				ps.setNull(++i, Types.BOOLEAN);
				ps.setNull(++i, Types.BOOLEAN);
				ps.setNull(++i, Types.INTEGER);
				ps.setNull(++i, Types.VARCHAR);
			}
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of STORAGE record failed");
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
	
	public static boolean update(StorageDevice d) {
		log.trace("Updating " + d);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE STORAGE SET "
					+ "NAME = ?, PATH = ?, IS_RECONCILED = ?, TYPE = ?, IS_AUTONOMOUS = ?, "
					+ "USED = ?, LABEL = ?, UUID = ?, IS_CONNECTED = ?, IS_MOUNTED = ?,  "
					+ "SIZE = ?, DEVICE_PATH = ?, LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			int i = 0;
			ps.setString(++i, d.getName());
			ps.setString(++i, d.getPath());
			ps.setBoolean(++i, d.isReconciled());
			ps.setString(++i, d.getType());
			ps.setBoolean(++i, d.isAutonomous());
			if (d instanceof HardDrive) {
				HardDrive hd = (HardDrive) d;
				if (hd.getUsed() != null) ps.setLong(++i, hd.getUsed()); else ps.setLong(++i, 0);
				ps.setString(++i, hd.getLabel());
				ps.setString(++i, hd.getUuid());
				ps.setBoolean(++i, hd.isConnected());
				ps.setBoolean(++i, hd.isMounted());
				if (hd.getSize() != null) ps.setLong(++i, hd.getSize()); else ps.setLong(++i, 0);
				ps.setString(++i, hd.getDevicePath());
			} else {
				ps.setNull(++i, Types.INTEGER);
				ps.setNull(++i, Types.VARCHAR);
				ps.setNull(++i, Types.VARCHAR);
				ps.setNull(++i, Types.BOOLEAN);
				ps.setNull(++i, Types.BOOLEAN);
				ps.setNull(++i, Types.INTEGER);
				ps.setNull(++i, Types.VARCHAR);
			}
			ps.setLong(++i, d.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of STORAGE record failed");
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
	
	/*public static boolean setUserDriveActive(long userId, long driveId, boolean isActive) {
		log.trace("Seting user ID " + userId + " drive ID " + driveId + " active flag to " + isActive);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE USER_STORAGE SET IS_ACTIVE=? WHERE STORAGE_ID=? AND USER_ID=?;");
			ps.setBoolean(1, isActive);
			ps.setLong(2, driveId);
			ps.setLong(3, userId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of USER_STORAGE record failed");
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
	}*/
	
	public static boolean delete(long driveId) {
		log.trace("Deleting Drive ID " + driveId);

		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM STORAGE WHERE ID = ?;");
			ps.setLong(1, driveId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of STORAGE record failed");
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
	
	public static boolean insertUserDrive(long userId, long driveId) {
		log.debug("Creating User Drive record for user ID " + userId + " and Drive ID " + driveId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO USER_STORAGE (USER_ID, STORAGE_ID, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES (?, ?, SYSDATE, SYSDATE);");
			ps.setLong(1, userId);
			ps.setLong(2, driveId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of USER_STORAGE record failed");
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
	
	public static boolean deleteUserDrive(long userId, long driveId) {
		log.debug("Deleting User Drive record for user ID " + userId + " and Drive ID " + driveId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM USER_STORAGE "
					+ "WHERE USER_ID=? "
					+ "AND STORAGE_ID=?;");
			ps.setLong(1, userId);
			ps.setLong(2, driveId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of USER_STORAGE record failed");
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
	
	public static boolean setUserDrives(long userId, List<StorageDevice> devices) {
		log.trace("Updating user drives for user ID " + userId);
		
		Connection con = null;
		
		String dIds = "";
		for (StorageDevice hd : devices) {
			dIds += hd.getId() != null ? hd.getId() + ", " : "";
		}
		dIds = !dIds.isEmpty() ? dIds.substring(0, dIds.length() - 2) : ""; // trim last comma
		
		try {
			con = HikariConnectionPool.getConnection();
			con.prepareStatement(
					"DELETE FROM USER_STORAGE WHERE USER_ID=" + userId 
					+ (!dIds.isEmpty() ? " AND STORAGE_ID NOT IN(" + dIds + ")" : "")
					+ ";").executeUpdate();
			
			for (StorageDevice hd : devices) {
				if (getUserDrive(hd.getId(), userId) == null) {
					if (!insertUserDrive(userId, hd.getId())) {
						con.close();
						throw new SQLException("Insert of USER_STORAGE record failed.");
					}
				}
			}
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	static List<StorageDevice> toStorageDeviceList(ResultSet rs) {
		List<StorageDevice> result = new ArrayList<StorageDevice>();
		try {
			List<String> columns = JdbcHelper.getResultColumns(rs);
			while (rs.next()) 
				result.add(toStorageDevice(rs, columns));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static StorageDevice toStorageDevice(ResultSet row) {
		return toStorageDevice(row, JdbcHelper.getResultColumns(row));
	}
	
	static StorageDevice toStorageDevice(ResultSet row, List<String> columns) {
		try {
			Long id = columns.contains("ID") ? row.getLong("ID") : null;
			String name = columns.contains("NAME") ? row.getString("NAME") : null;
			String path = columns.contains("PATH") ? row.getString("PATH") : null;
			String devicePath = columns.contains("DEVICE_PATH") ? row.getString("DEVICE_PATH") : null;
			String label = columns.contains("LABEL") ? row.getString("LABEL") : null;
			String uuid = columns.contains("UUID") ? row.getString("UUID") : null;
			String type = columns.contains("TYPE") ? row.getString("TYPE") : null;
			Boolean connected = columns.contains("IS_CONNECTED") ? row.getBoolean("IS_CONNECTED") : null;
			Boolean mounted = columns.contains("IS_MOUNTED") ? row.getBoolean("IS_MOUNTED") : null;
			Boolean reconciled = columns.contains("IS_RECONCILED") ? row.getBoolean("IS_RECONCILED") : null;
			Boolean autonomous = columns.contains("IS_AUTONOMOUS") ? row.getBoolean("IS_AUTONOMOUS") : null;
			Long size = columns.contains("SIZE") ? row.getLong("SIZE") : null;
			Long used = columns.contains("USED") ? row.getLong("USED") : null;
			Date created = columns.contains("CREATE_DATE") ? row.getTimestamp("CREATE_DATE") : null;
			Date updated = columns.contains("LAST_UPDATE_DATE") ? row.getTimestamp("LAST_UPDATE_DATE") : null;
			
			if (type != null && type.equals(FilesystemLocation.TYPE)) {
				return new FilesystemLocation(id, name, path, reconciled, autonomous, created, updated);
			} else {
				return new HardDrive(id, name, path, devicePath, label, uuid, type, connected, mounted, reconciled, autonomous, size, used, created, updated);
			}
		} catch (SQLException e) {
			log.error(e, e);
			return null;
		}
	}
	
	public static boolean resetReconciliation() {
		
		Connection con = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			con.prepareStatement(
					"UPDATE STORAGE SET IS_RECONCILED = FALSE, LAST_UPDATE_DATE = SYSDATE;").executeUpdate();
			con.prepareStatement(
					"UPDATE FILE SET IS_RECONCILED = FALSE, LAST_UPDATE_DATE = SYSDATE;").executeUpdate();
			
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
}
