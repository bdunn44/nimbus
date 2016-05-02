package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.Album;
import com.kbdunn.nimbus.common.model.Artist;
import com.kbdunn.nimbus.common.model.FilesystemLocation;
import com.kbdunn.nimbus.common.model.Playlist;
import com.kbdunn.nimbus.common.model.Song;
import com.kbdunn.nimbus.common.model.Video;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;
import com.kbdunn.nimbus.server.jdbc.JdbcHelper;

public abstract class MediaLibraryDAO {
	
	private static final Logger log = LogManager.getLogger(MediaLibraryDAO.class.getName());
	
	// Retrieve a media group with the given ID from DB
	public static Playlist getPlaylistById(long id) {
		log.trace("Getting playlist with ID " + id);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM PLAYLIST WHERE ID = ?;");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("Playlist ID " + id + " was not found");
				return null;
			}
			return toPlaylist(rs);
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
	
	// Retrieve a media group with the given name and type from DB
	public static Playlist getPlaylistByName(long userId, String name) {
		log.trace(String.format("Getting Playlist %s. User ID: %s", name, userId));
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM PLAYLIST WHERE "
					+ "USER_ID = ? AND NAME = ?;");
			ps.setLong(1, userId);
			ps.setString(2, name);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.debug("Group was not found");
				return null;
			}
			
			return toPlaylist(rs);
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
	
	// Retrieve an Artist with the given name and owner
	public static Artist getArtistByName(long userId, String name) {
		log.debug(String.format("Getting Artist %s. User ID: %s", name, userId));
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.ARTIST FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON ud.STORAGE_ID = d.ID "
					+ "WHERE ud.USER_ID = ? "
					+ "AND f.IS_SONG "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?)"
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "AND f.ARTIST" + (!name.equals(Artist.UNKNOWN) ? "= ? " : " IS NULL ")
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (!name.equals(Artist.UNKNOWN)) ps.setString(4, name);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.debug("Artist was not found");
				return null;
			}
			
			return new Artist(rs.getString("ARTIST"));
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
	
	// Retrieve an Album with the given name and owner
	public static Album getAlbumByName(long userId, String artistName, String albumName) {
		log.trace(String.format("Getting Album %s by %s. User ID: %s", albumName, artistName, userId));
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.ARTIST, f.ALBUM FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON ud.STORAGE_ID = d.ID "
					+ "WHERE ud.USER_ID = ? "
					+ "AND f.IS_SONG "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?)"
					+ "AND f.ARTIST" + (!artistName.equals(Artist.UNKNOWN) ? "= ? " : " IS NULL ")
					+ "AND f.ALBUM" + (!albumName.equals(Album.UNKNOWN) ? "= ? " : " IS NULL ")
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ";");
			int i = 0;
			ps.setLong(++i, userId);
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
			if (!artistName.equals(Artist.UNKNOWN)) ps.setString(++i, artistName);
			if (!albumName.equals(Album.UNKNOWN)) ps.setString(++i, albumName);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.debug("Album was not found");
				return null;
			}
			
			return new Album(rs.getString("ALBUM"), rs.getString("ARTIST"));
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
	
	public static List<Artist> getUserArtists(long userId) {
		return getUserArtists(userId, null, null);
	}
	
	public static List<Artist> getUserArtists(long userId, Integer startIndex, Integer count) {
		log.trace("Retrieving artists for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.ARTIST FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE ud.USER_ID = ? "
					+ "AND f.IS_SONG "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY NVL(f.ARTIST, 'ZZZ')" // NULLs come last
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(4, startIndex);
				ps.setInt(5, count);
			}
			rs = ps.executeQuery();
			return toArtistList(rs);
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
	
	public static int getUserArtistCount(long userId) {
		log.trace("Retrieving artist count for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM ("
						+ "SELECT DISTINCT f.ARTIST FROM FILE f "
						+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
						+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
						+ "WHERE ud.USER_ID = ? "
						+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
							+ "OR d.TYPE = ?) "
						+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ");");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() query did not return a row");
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
	
	public static List<Album> getUserAlbums(long userId) {
		return getUserAlbums(userId, null, null);
	}
	
	public static List<Album> getUserAlbums(long userId, Integer startIndex, Integer count) {
		log.trace("Retrieving albums for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.ARTIST, f.ALBUM FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE ud.USER_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY NVL(f.ALBUM, 'ZZZ')"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(4, startIndex);
				ps.setInt(5, count);
			}
			rs = ps.executeQuery();
			return toAlbumList(rs);
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
	
	public static int getUserAlbumCount(long userId) {
		log.trace("Retrieving album count for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM ("
						+ "SELECT DISTINCT f.ARTIST, f.ALBUM FROM FILE f "
						+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
						+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
						+ "WHERE ud.USER_ID = ? "
						+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
							+ "OR d.TYPE = ?) "
						+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ");");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() query did not return a row");
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
	
	public static List<Playlist> getUserPlaylists(long userId) {
		return getUserPlaylists(userId, null, null);
	}
	
	public static List<Playlist> getUserPlaylists(long userId, Integer startIndex, Integer count) {
		log.trace("Retrieving playlists for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT p.* "
					+ "FROM PLAYLIST p "
					+ "WHERE p.USER_ID = ? "
					+ "ORDER BY NAME"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, userId);
			if (startIndex != null) {
				ps.setInt(2, startIndex);
				ps.setInt(3, count);
			}
			rs = ps.executeQuery();
			return toPlaylistList(rs);
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
	
	public static int getUserPlaylistCount(long userId) {
		log.trace("Retrieving playlist count for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM PLAYLIST WHERE USER_ID = ?;");
			ps.setLong(1, userId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() query did not return a row");
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

	// Retrieve all songs for a given User
	public static List<Song> getUserSongs(long userId) {
		return getUserSongs(userId, null, null);
	}
	
	// Retrieve all songs for a given User
	public static List<Song> getUserSongs(long userId, Integer startIndex, Integer count) {
		log.trace("Retrieving songs for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG AND ud.USER_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY NVL(f.ARTIST, 'ZZZ'), NVL(f.ALBUM, 'ZZZ'), TRACK_NO, TITLE, PATH "
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(4, startIndex);
				ps.setInt(5, count);
			}
			rs = ps.executeQuery();
			return toSongList(rs);
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
	
	public static int getUserSongCount(long userId) {
		log.trace("Retrieving song count for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM ("
						+ "SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID "
						+ "FROM FILE f "
						+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
						+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
						+ "WHERE f.IS_SONG AND ud.USER_ID = ? "
						+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
							+ "OR d.TYPE = ?) "
						+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ");");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() query did not return a row");
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
	
	// Retrieve all videos for a given User
	public static List<Video> getUserVideos(long userId) {
		return getUserVideos(userId, null, null);
	}
	
	// Retrieve all videos for a given User
	public static List<Video> getUserVideos(long userId, Integer startIndex, Integer count) {
		log.trace("Retrieving videos for user: " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID  "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_VIDEO AND ud.USER_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY TITLE, PATH"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(4, startIndex);
				ps.setInt(5, count);
			}
			rs = ps.executeQuery();
			return toVideoList(rs);
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
	
	public static int getUserVideoCount(long userId) {
		log.trace("Retrieving video count for user: " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1) FROM ("
						+ "SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID  "
						+ "FROM FILE f "
						+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
						+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
						+ "WHERE f.IS_VIDEO AND ud.USER_ID = ? "
						+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
							+ "OR d.TYPE = ?) "
						+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ");");
			ps.setLong(1, userId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("A COUNT() query did not return a row");
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
	
	public static List<Song> getPlaylistSongs(long playlistId) {
		return getPlaylistSongs(playlistId, null, null);
	}
	
	// Get the contents of a given media group
	// Artist contents are retreived by a different method
	public static List<Song> getPlaylistSongs(long playlistId, Integer startIndex, Integer count) {
		log.trace("Getting contents of playlist ID " + playlistId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT f.*, ud.USER_ID, ud.STORAGE_ID  "
					+ "FROM FILE f "
					+ "JOIN PLAYLIST_FILE i ON f.ID = i.FILE_ID "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND i.PLAYLIST_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY i.PLAY_ORDER, i.ID"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			ps.setLong(1, playlistId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(4, startIndex);
				ps.setInt(5, count);
			}
			rs = ps.executeQuery();
			return toSongList(rs);
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
	
	public static int getPlaylistSongCount(long playlistId) {
		log.trace("Getting content count of playlist ID " + playlistId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1)  "
					+ "FROM FILE f "
					+ "JOIN PLAYLIST_FILE i ON f.ID = i.FILE_ID "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND i.PLAYLIST_ID = ? "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ";");
			ps.setLong(1, playlistId);
			ps.setString(2, FilesystemLocation.TYPE);
			ps.setString(3, FilesystemLocation.TYPE);
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
	
	public static List<Song> getArtistSongs(long userId, Artist artist) {
		return getArtistSongs(userId, artist, null, null);
	}
	
	public static List<Song> getArtistSongs(long userId, Artist artist, Integer startIndex, Integer count) {
		log.debug("Getting songs for artist " + artist);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID  "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND ud.USER_ID = ? "
					+ "AND f.ARTIST" + (!artist.getName().equals(Artist.UNKNOWN) ? "=?" : " IS NULL") + " "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY NVL(f.ALBUM, 'ZZZ'), f.TRACK_NO, f.TITLE, f.PATH"
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			int i = 0;
			ps.setLong(++i, userId);
			if (!artist.getName().equals(Artist.UNKNOWN)) ps.setString(++i, artist.getName());
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(++i, startIndex);
				ps.setInt(++i, count);
			}
			rs = ps.executeQuery();
			return toSongList(rs);
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
	
	public static int getArtistSongCount(long userId, Artist artist) {
		log.debug("Getting song count for artist " + artist);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1)  "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND ud.USER_ID = ? "
					+ "AND f.ARTIST" + (!artist.getName().equals(Artist.UNKNOWN) ? "= ? " : " IS NULL ")
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ ";");
			int i = 0;
			ps.setLong(++i, userId);
			if (!artist.getName().equals(Artist.UNKNOWN)) ps.setString(++i, artist.getName());
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
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
	
	public static List<Song> getAlbumSongs(long userId, Album album) {
		return getAlbumSongs(userId, album, null, null);
	}
	
	public static List<Song> getAlbumSongs(long userId, Album album, Integer startIndex, Integer count) {
		log.trace("Getting songs for album " + album);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.*, ud.USER_ID, ud.STORAGE_ID  "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND ud.USER_ID = ? "
					+ "AND f.ARTIST" + (!album.getArtistName().equals(Artist.UNKNOWN) ? "= ? " : " IS NULL ")
					+ "AND f.ALBUM" + (!album.getName().equals(Album.UNKNOWN) ? "= ? " : " IS NULL ")
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY f.TRACK_NO, f.TITLE, f.PATH "
					+ (startIndex != null ? " OFFSET ? LIMIT ?" : "")
					+ ";");
			int i = 0;
			ps.setLong(++i, userId);
			if (!album.getArtistName().equals(Artist.UNKNOWN)) ps.setString(++i, album.getArtistName());
			if (!album.getName().equals(Album.UNKNOWN)) ps.setString(++i, album.getName());
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
			if (startIndex != null) {
				ps.setInt(++i, startIndex);
				ps.setInt(++i, count);
			}
			rs = ps.executeQuery();
			return toSongList(rs);
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
	
	public static int getAlbumSongCount(long userId, Album album) {
		log.trace("Getting song count for album " + album);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT COUNT(1)  "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND ud.USER_ID = ? "
					+ "AND f.ARTIST" + (!album.getArtistName().equals(Artist.UNKNOWN) ? " = ? " : " IS NULL ")
					+ "AND f.ALBUM" + (!album.getName().equals(Album.UNKNOWN) ? " = ? " : " IS NULL ")
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ ";");
			int i = 0;
			ps.setLong(++i, userId);
			if (!album.getArtistName().equals(Artist.UNKNOWN)) ps.setString(++i, album.getArtistName());
			if (!album.getName().equals(Album.UNKNOWN)) ps.setString(++i, album.getName());
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
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
	
	// Get child albums of an Artist
	public static List<Album> getArtistAlbums(long userId, Artist artist) {
		log.trace("Getting albums for " + artist);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT DISTINCT f.ARTIST, f.ALBUM "
					+ "FROM FILE f "
					+ "JOIN USER_STORAGE ud ON f.USER_STORAGE_ID = ud.ID "
					+ "JOIN STORAGE d ON d.ID = ud.STORAGE_ID "
					+ "WHERE f.IS_SONG "
					+ "AND ud.USER_ID = ? "
					+ "AND f.ARTIST" + (!artist.getName().equals(Artist.UNKNOWN) ? " = ? " : " IS NULL ")
					+ "AND ((d.TYPE <> ? AND d.IS_MOUNTED AND d.IS_CONNECTED) "
						+ "OR d.TYPE = ?) "
					+ "AND NOT f.IS_LIBRARY_REMOVED "
					+ "ORDER BY NVL(f.ALBUM, 'ZZZ');");
			int i = 0;
			ps.setLong(++i, userId);
			if (!artist.getName().equals(Artist.UNKNOWN)) ps.setString(++i, artist.getName());
			ps.setString(++i, FilesystemLocation.TYPE);
			ps.setString(++i, FilesystemLocation.TYPE);
			rs = ps.executeQuery();
			
			return toAlbumList(rs);
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
	
	// Insert a new group - content's play order is the same as the list order
	public static boolean insertPlaylist(Playlist playlist) {
		log.trace("Inserting playlist" + playlist);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO PLAYLIST "
					+ "(USER_ID, NAME, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, SYSDATE, SYSDATE);");
			ps.setLong(1, playlist.getUserId());
			ps.setString(2, playlist.getName());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of PLAYLIST record failed");
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
	
	public static boolean insertPlaylistSongs(long playlistId, List<Song> songs) {
		log.trace("Inserting song references for playlist ID " + playlistId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO PLAYLIST_FILE "
					+ "(PLAYLIST_ID, FILE_ID, PLAY_ORDER, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, SYSDATE, SYSDATE);");
			ps.setLong(1, playlistId);
			
			int playOrder = 0;
			for (Song song : songs) {
				ps.setLong(2, song.getId());
				ps.setLong(3, ++playOrder);
				if (ps.executeUpdate() != 1) {
					ps.close(); 
					con.close();
					throw new SQLException("Failed to insert group item!");
				}
			}
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
	
	public static boolean updatePlaylist(Playlist playlist) {
		log.trace("Updating playlist " + playlist);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE PLAYLIST "
					+ "SET NAME = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			ps.setString(1, playlist.getName());
			ps.setLong(2, playlist.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of PLAYLIST record failed");
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
	
	public static boolean deletePlaylist(long playlistId) {
		log.trace("Deleting playlist ID " + playlistId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM PLAYLIST WHERE ID = ?;");
			ps.setLong(1, playlistId);

			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of PLAYLIST record failed");
			
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
	
	public static boolean deletePlaylistSongs(long playlistId) {
		log.trace("Deleting songs for playlist ID " + playlistId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM PLAYLIST_FILE "
					+ "WHERE PLAYLIST_ID = ?;");
			ps.setLong(1, playlistId);
			ps.executeUpdate();
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
	
	// Converts a SQL ResultSet into a list of Songs
	public static List<Song> toSongList(ResultSet rs) {
		List<Song> songs = new ArrayList<Song>();
		List<String> columns = JdbcHelper.getResultColumns(rs);
		try {
			while (rs.next()) {
				songs.add((Song) NimbusFileDAO.toNimbusFile(rs, columns));
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return songs;
	}
	
	// Converts a SQL ResultSet into a list of Videos
	public static List<Video> toVideoList(ResultSet rs) {
		List<Video> videos = new ArrayList<Video>();
		List<String> columns = JdbcHelper.getResultColumns(rs);
		try {
			while (rs.next()) {
				videos.add((Video) NimbusFileDAO.toNimbusFile(rs, columns));
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return videos;
	}
	
	// Converts a SQL ResultSet into a list of Artists
	public static List<Artist> toArtistList(ResultSet rs) {
		List<Artist> artists = new ArrayList<Artist>();
		try {
			while (rs.next()) {
				artists.add(new Artist(rs.getString("ARTIST")));
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return artists;
	}
	
	// Converts a SQL ResultSet into a list of Artists
	public static List<Album> toAlbumList(ResultSet rs) {
		List<Album> albums = new ArrayList<Album>();
		try {
			while (rs.next()) {
				albums.add(new Album(rs.getString("ALBUM"), rs.getString("ARTIST")));
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return albums;
	}
	
	// Converts a SQL ResultSet into a list of Artists
	static List<Playlist> toPlaylistList(ResultSet rs) {
		List<Playlist> groups = new ArrayList<Playlist>();
		List<String> columns = JdbcHelper.getResultColumns(rs);
		try {
			while (rs.next()) {
				groups.add((Playlist) toPlaylist(rs, columns));
			}
		} catch (SQLException e) {
			log.error(e, e);
		}
		return groups;
	}
	
	static Playlist toPlaylist(ResultSet rs) {
		return toPlaylist(rs, JdbcHelper.getResultColumns(rs));
	}
	
	// Converts a single row in a ResultSet to a NimbusFile
	static Playlist toPlaylist(ResultSet rs, List<String> rsColumns) {
		try {
			Long id = rs.getLong("ID");
			Long ownerId = rs.getLong("USER_ID");
			String name = rs.getString("NAME");
			Date createDate = rs.getTimestamp("CREATE_DATE");
			Date updateDate = rs.getTimestamp("LAST_UPDATE_DATE");
			
			return new Playlist(id, ownerId, name, createDate, updateDate);
			
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
}
