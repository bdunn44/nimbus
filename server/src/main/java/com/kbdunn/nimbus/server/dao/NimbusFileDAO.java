package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.jdbc.JdbcHelper;

public abstract class NimbusFileDAO {
	
	private static Logger log = LogManager.getLogger(NimbusFileDAO.class.getName());
	
	// Get a file from DB by path. 
	public static NimbusFile getByPath(String path) {
		log.trace("Getting FILE with path " + path);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT f.*, ud.USER_ID, ud.STORAGE_ID FROM FILE f JOIN USER_STORAGE ud ON ud.ID = f.USER_STORAGE_ID " +
					"WHERE PATH=? ");
			ps.setString(1, path);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.trace("FILE " + path + " not found.");
				return null;
			}
			
			return toNimbusFile(rs, JdbcHelper.getResultColumns(rs));
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
	
	public static NimbusFile getById(Long id) {
		log.trace("Getting FILE with ID " + id);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT f.*, ud.USER_ID, ud.STORAGE_ID FROM FILE f JOIN USER_STORAGE ud ON ud.ID = f.USER_STORAGE_ID " +
					"WHERE ID=? ");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.debug("FILE with ID " + id + " not found.");
				return null;
			}
			
			return toNimbusFile(rs, JdbcHelper.getResultColumns(rs));
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
	
	public static long getTotalFileCount() {
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM FILE;");
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A count() query did not return a row");
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

	public static long getTotalFileSize() {
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT SUM(SIZE) FROM FILE;");
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A count() query did not return a row");
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
	
	public static long getLastReconciled(long id) {
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT LAST_RECONCILED FROM FILE WHERE ID = ?;");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("File ID does not exist");
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
	
	// Get all contents
	public static List<NimbusFile> getContents(NimbusFile folder) {
		return getContents(folder, false, false, false, null, null);
	}
	
	// Get all contents within a given range
	public static List<NimbusFile> getContents(NimbusFile folder, int startIndex, int count) {
		return getContents(folder, false, false, false, startIndex, count);
	}
	
	// Get child files
	public static List<NimbusFile> getFileContents(NimbusFile folder) {
		return getContents(folder, true, false, false, null, null);
	}
	
	// Get child folders
	public static List<NimbusFile> getFolderContents(NimbusFile folder) {
		return getContents(folder, false, true, false, null, null);
	}
	
	// Get child images
	public static List<NimbusFile> getImageContents(NimbusFile folder) {
		return getContents(folder, true, false, true, null, null);
	}
	
	// Get folder contents
	private static List<NimbusFile> getContents(NimbusFile folder, boolean excludeDirectories, boolean excludeRegularFiles, boolean imageFilesOnly, 
			Integer startIndex, Integer count) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT f.*, NAME, ud.USER_ID, ud.STORAGE_ID FROM FILE f JOIN USER_STORAGE ud ON ud.ID = f.USER_STORAGE_ID "
					+ "WHERE PARENT_PATH = ? "
					+ (excludeDirectories ? " AND NOT IS_DIRECTORY" : "")
					+ (excludeRegularFiles ? " AND IS_DIRECTORY" : "")
					+ (imageFilesOnly ? " AND IS_IMAGE" : "") 
					+ " ORDER BY IS_DIRECTORY DESC, NAME"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setString(1, folder.getPath());
			if (startIndex != null) {
				ps.setInt(2, startIndex);
				ps.setInt(3, count);
			}
			
			rs = ps.executeQuery();
			return toNimbusFileList(rs);
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
	
	// Get content size
	public static int getContentCount(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(0) FROM FILE "
					+ "WHERE PARENT_PATH = ?;");
			ps.setString(1, folder.getPath());
			
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A count() query did not return a row");
			return rs.getInt(1);
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
	
	public static int getRecursiveChildCount(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM FILE "
					+ "WHERE PATH LIKE ? " // Paths that start with the parent path
					+ "AND NOT IS_DIRECTORY;");
			ps.setString(1, folder.getPath() + "%");
			
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() function did not return any rows.");
			return rs.getInt(1); // Should never be null
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
		return 0;
	}
	
	public static long getRecursiveFileSize(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT SUM(SIZE) FROM FILE "
					+ "WHERE PATH LIKE ? " // Paths that start with the parent path
					+ "AND NOT IS_DIRECTORY;");
			ps.setString(1, folder.getPath() + "%");
			
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A SUM() function did not return any rows.");
			return rs.getLong(1); // Should never be null
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
		return 0;
	}
	
	public static boolean fileHasChildren(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"VALUES EXISTS (SELECT 0 FROM FILE WHERE PARENT_PATH = ?);"
				);
			ps.setString(1, folder.getPath());
			
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A VALUES query did not return a row");
			return rs.getBoolean(1);
			
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
	
	public static boolean fileHasChildFolders(NimbusFile folder) {
		if (!folder.isDirectory()) throw new IllegalArgumentException("Argument must be a directory");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"VALUES EXISTS (SELECT 0 FROM FILE WHERE PARENT_PATH = ? AND IS_DIRECTORY);"
				);
			ps.setString(1, folder.getPath());
			
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A VALUES query did not return a row");
			return rs.getBoolean(1);
			
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
	
	public static boolean insert(NimbusFile nf) {
		//NimbusFile workingFile = nf; // Don't lose the pointer to the original file if isSong or isVideo
		// The instantiation of the actual NimbusFile object by the Service should take care of this
		//if (workingFile.isSong() && !(workingFile instanceof Song)) workingFile = new Song(workingFile);
		//if (workingFile.isVideo() && !(workingFile instanceof Video)) workingFile = new Video(workingFile);
		
		log.debug("Creating file " + nf + " in database" 
				+ (nf.isSong() ? " (Song)" : nf.isVideo() ? " (Video)" : ""));
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			// Insert File
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO FILE "
					+ "(USER_STORAGE_ID, PATH, SIZE, IS_DIRECTORY, IS_RECONCILED, LAST_RECONCILED, IS_SONG, IS_VIDEO, IS_IMAGE, IS_LIBRARY_REMOVED, "
					+ "TITLE, SEC_LENGTH, TRACK_NO, ARTIST, ALBUM, ALBUM_YEAR, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "((SELECT ID FROM USER_STORAGE WHERE USER_ID=? AND STORAGE_ID=?), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			int i = 0;
			ps.setLong(++i, nf.getUserId());
			ps.setLong(++i, nf.getStorageDeviceId());
			ps.setString(++i, nf.getPath());
			ps.setLong(++i, nf.getSize());
			ps.setBoolean(++i, nf.isDirectory());
			ps.setBoolean(++i, nf.isReconciled());
			ps.setLong(++i, nf.getLastReconciled() == null ? 0 : nf.getLastReconciled());
			ps.setBoolean(++i, nf.isSong());
			ps.setBoolean(++i, nf.isVideo());
			ps.setBoolean(++i, nf.isImage());
			ps.setBoolean(++i, nf.isLibraryRemoved()); 
			
			if (nf instanceof Song) {
				Song ns = (Song) nf;
				if (!ns.getTitle().equals(ns.getName())) {
					ps.setString(++i, ns.getTitle());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (ns.getLength() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, ns.getLength());
				}
				if (ns.getTrackNumber() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, ns.getTrackNumber());
				}
				if (!ns.getArtist().equals(Artist.UNKNOWN)) {
					ps.setString(++i, ns.getArtist());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (!ns.getAlbum().equals(Album.UNKNOWN)) {
					ps.setString(++i, ns.getAlbum());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				ps.setString(++i, ns.getAlbumYear());
			} else if (nf instanceof Video) {
				Video nv = (Video)nf;
				if (!nv.getTitle().equals(nv.getName())) {
					ps.setString(++i, nv.getTitle());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (nv.getLength() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, nv.getLength());
				}
				ps.setNull(++i, Types.INTEGER); // Track Number
				ps.setString(++i, null); // Artist
				ps.setString(++i, null); // Album
				ps.setString(++i, null); // Album Year
			} else {
				ps.setString(++i, null); // Title
				ps.setNull(++i, Types.INTEGER); // Length
				ps.setNull(++i, Types.INTEGER); // Track Number
				ps.setString(++i, null); // Artist
				ps.setString(++i, null); // Album
				ps.setString(++i, null); // Album Year
			}
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of FILE record failed");
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
	
	// Update a file in the DB
	public static boolean update(NimbusFile nf) {
		if (nf.getId() == null) throw new IllegalArgumentException("Cannot update FILE record - ID is null");	
		
		//if (nf.isSong() && !(nf instanceof Song)) nf = new Song(nf);
		//if (nf.isVideo() && !(nf instanceof Video)) nf = new Video(nf);
		
		log.debug("Updating file " + nf + " in database");
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE FILE SET "
					+ "USER_STORAGE_ID = (SELECT ID FROM USER_STORAGE WHERE USER_ID=? AND STORAGE_ID=?), "
					+ "PATH = ?, "
					+ "SIZE = ?, "
					+ "IS_DIRECTORY = ?, "
					+ "IS_RECONCILED = ?, "
					+ "LAST_RECONCILED = ?, "
					+ "IS_SONG = ?, "
					+ "IS_VIDEO = ?, "
					+ "IS_IMAGE = ?, "
					+ "IS_LIBRARY_REMOVED = ?, "
					+ "TITLE = ?, "
					+ "SEC_LENGTH = ?, "
					+ "TRACK_NO = ?, "
					+ "ARTIST = ?, "
					+ "ALBUM = ?, "
					+ "ALBUM_YEAR = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			int i = 0;
			ps.setLong(++i, nf.getUserId());
			ps.setLong(++i, nf.getStorageDeviceId());
			ps.setString(++i, nf.getPath());
			ps.setLong(++i, nf.getSize());
			ps.setBoolean(++i, nf.isDirectory());
			ps.setBoolean(++i, nf.isReconciled());
			ps.setLong(++i, nf.getLastReconciled() == null ? 0 : nf.getLastReconciled());
			ps.setBoolean(++i, nf.isSong());
			ps.setBoolean(++i, nf.isVideo());
			ps.setBoolean(++i, nf.isImage());
			ps.setBoolean(++i, nf.isLibraryRemoved());
			
			if (nf instanceof Song) {
				Song ns = (Song) nf;
				if (!ns.getTitle().equals(ns.getName())) {
					ps.setString(++i, ns.getTitle());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (ns.getLength() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, ns.getLength());
				}
				if (ns.getTrackNumber() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, ns.getTrackNumber());
				}
				if (!ns.getArtist().equals(Artist.UNKNOWN)) {
					ps.setString(++i, ns.getArtist());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (!ns.getAlbum().equals(Album.UNKNOWN)) {
					ps.setString(++i, ns.getAlbum());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				ps.setString(++i, ns.getAlbumYear());
			} else if (nf instanceof Video) {
				Video nv = (Video)nf;
				if (!nv.getTitle().equals(nv.getName())) {
					ps.setString(++i, nv.getTitle());
				} else {
					ps.setNull(++i, Types.VARCHAR);
				}
				if (nv.getLength() == null) {
					ps.setNull(++i, Types.INTEGER);
				} else {
					ps.setInt(++i, nv.getLength());
				}
				ps.setNull(++i, Types.INTEGER); // Track Number
				ps.setString(++i, null); // Artist
				ps.setString(++i, null); // Album
				ps.setString(++i, null); // Album Year
			} else {
				ps.setString(++i, null); // Title
				ps.setNull(++i, Types.INTEGER); // Length
				ps.setNull(++i, Types.INTEGER); // Track Number
				ps.setString(++i, null); // Artist
				ps.setString(++i, null); // Album
				ps.setString(++i, null); // Album Year
			}
			ps.setLong(++i, nf.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of FILE record failed");
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
	
	// Delete a file from the DB
	public static boolean delete(NimbusFile nf) {
		if (nf.getId() == null) throw new IllegalArgumentException("Cannot delete FILE record - ID is null");	
		log.debug("Deleting file from database " + nf);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM FILE WHERE ID = ?");
			ps.setLong(1, nf.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of FILE record failed");
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
	
	// Converts a SQL ResultSet into a list of NimbusFiles and NimbusSongs
	public static List<NimbusFile> toNimbusFileList(ResultSet rs) {
		List<NimbusFile> files = new ArrayList<NimbusFile>();
		List<String> columns = JdbcHelper.getResultColumns(rs);
		try {
			while (rs.next()) {
				files.add(toNimbusFile(rs, columns));
			}
		} catch (SQLException e) {
			log.error(e, e);
		} 
		return files;
	}
	
	// Converts a single row in a ResultSet to a NimbusFile
	// Owner and hard drive aren't strictly required, but eliminate two DB queries
	public static NimbusFile toNimbusFile(ResultSet rs, List<String> rsColumns) {
		try {
			Long id = rsColumns.contains("ID") ? rs.getLong("ID") : null;
			Long userId = rsColumns.contains("USER_ID") ? rs.getLong("USER_ID") : null;
			Long driveId = rsColumns.contains("STORAGE_ID") ? rs.getLong("STORAGE_ID") : null;
			String path = rsColumns.contains("PATH") ? rs.getString("PATH") : null;
			Boolean isDir = rsColumns.contains("IS_DIRECTORY") ? rs.getBoolean("IS_DIRECTORY") : null;
			Long size = rsColumns.contains("SIZE") ? rs.getLong("SIZE") : null;
			Boolean isSong = rsColumns.contains("IS_SONG") ? rs.getBoolean("IS_SONG") : null;
			Boolean isVideo = rsColumns.contains("IS_VIDEO") ? rs.getBoolean("IS_VIDEO") : null;
			Boolean isImage = rsColumns.contains("IS_IMAGE") ? rs.getBoolean("IS_IMAGE") : null;
			String artist = rsColumns.contains("ARTIST") ? rs.getString("ARTIST") : null;
			String album = rsColumns.contains("ALBUM") ? rs.getString("ALBUM") : null;
			String title = rsColumns.contains("TITLE") ? rs.getString("TITLE") : null;
			Integer length = rsColumns.contains("SEC_LENGTH")
					&& rs.getObject("SEC_LENGTH") != null ? rs.getInt("SEC_LENGTH") : null;
			String year = rsColumns.contains("ALBUM_YEAR") ? rs.getString("ALBUM_YEAR") : null;
			Integer trackNo = rsColumns.contains("TRACK_NO") 
					&& rs.getObject("TRACK_NO") != null ? rs.getInt("TRACK_NO") : null;
			Boolean isReconciled = rsColumns.contains("IS_RECONCILED") ? rs.getBoolean("IS_RECONCILED") : null;
			Long lastReconciled = rsColumns.contains("LAST_RECONCILED") ? rs.getLong("LAST_RECONCILED") : null;
			Boolean isLibraryRemoved = rsColumns.contains("IS_LIBRARY_REMOVED") ? rs.getBoolean("IS_LIBRARY_REMOVED") : null;
			Date createDate = rsColumns.contains("CREATE_DATE") ? rs.getTimestamp("CREATE_DATE") : null;
			Date updateDate = rsColumns.contains("LAST_UPDATE_DATE") ? rs.getTimestamp("LAST_UPDATE_DATE") : null;
			
			if (isSong != null && isSong) {
				return new Song(id, userId, driveId, path, isDir, size, isSong, 
						isVideo, isImage, isReconciled, lastReconciled, isLibraryRemoved, createDate, updateDate, title, length, trackNo, artist, album, year);
			} else if (isVideo != null && isVideo) {
				return new Video(id, userId, driveId, path, isDir, size, isSong, 
						isVideo, isImage, isReconciled, lastReconciled, isLibraryRemoved, createDate, updateDate, title, length);
			} else {
				return new NimbusFile(id, userId, driveId, path, isDir, size, isSong, 
						isVideo, isImage, isReconciled, lastReconciled, isLibraryRemoved, createDate, updateDate);
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
}