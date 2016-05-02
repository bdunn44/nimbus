package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.NimbusFile;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockAccess;
import com.kbdunn.nimbus.common.model.ShareBlockFile;
import com.kbdunn.nimbus.common.model.ShareBlockRecipient;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public abstract class ShareDAO {
	
	private static final Logger log = LogManager.getLogger(ShareDAO.class.getName());
	
	// Retrieves all  shares for a given user ID
	public static List<ShareBlock> getShareBlocksForUser(long userId) {
		log.trace("Getting all file shares for user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * "
					+ "FROM SHARE_BLOCK WHERE USER_ID = ?;");
			ps.setLong(1, userId);
			rs = ps.executeQuery();
			return toShareBlockList(rs);
			
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
	
	// Retrieves all  shares for a given user ID
	public static List<ShareBlock> getBlocksSharedWithUser(long userId) {
		log.trace("Getting all blocks shared with user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT sb.* "
					+ "FROM SHARE_BLOCK sb, SHARE_BLOCK_ACCESS sba "
					+ "WHERE sba.USER_ID = ? "
					+ "AND sb.ID = sba.SHARE_BLOCK_ID;");
			ps.setLong(1, userId);
			rs = ps.executeQuery();
			return toShareBlockList(rs);
			
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
	
	// Retrieves a  file share with a given token
	public static ShareBlock getShareByToken(String token) {
		log.trace("Retrieving file share with token: " + token);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * "
					+ "FROM SHARE_BLOCK WHERE TOKEN = ?;");
			ps.setString(1, token);
			rs = ps.executeQuery();
			
			if (!rs.next()) {
				log.warn("A share block with token '" + token + "' was not found");
				return null;
			}
			return toShareBlock(rs);
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
	
	// Retrieves  file share with a given ID
	public static ShareBlock getShareBlockById(long id) {
		log.trace("Getting share block with ID: " + id);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * "
					+ "FROM SHARE_BLOCK WHERE ID = ?;");
			ps.setLong(1, id);
			rs = ps.executeQuery();
			
			if (!rs.next()) throw new SQLException("A Share Block with ID '" + id + "' was not found");
			return toShareBlock(rs);
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
	
	// Gets the contents of a  file share
	public static List<ShareBlockFile> getSharedFiles(long shareBlockId) {
		log.trace("Getting the contents of share block ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT nsf.* "
					+ "FROM SHARE_BLOCK_FILE nsf "
					+ "WHERE nsf.SHARE_BLOCK_ID = ?;");
			ps.setLong(1, shareBlockId);
			
			rs = ps.executeQuery();
			return toShareBlockFileList(rs);
			
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
	
	// Gets the NimbusFile contents of a  file share
	public static List<NimbusFile> getSharedNimbusFiles(long shareBlockId) {
		log.trace("Getting the NimbusFile contents of share block ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT nf.*, ud.USER_ID, ud.STORAGE_ID FROM FILE nf JOIN USER_STORAGE ud ON ud.ID = nf.USER_STORAGE_ID  "
					+ "JOIN SHARE_BLOCK_FILE nsf ON nsf.FILE_ID = nf.ID "
					+ "WHERE nsf.SHARE_BLOCK_ID = ?;");
			ps.setLong(1, shareBlockId);
			
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
	
	// Get a specific shared file
	public static ShareBlockFile getSharedFile(long shareBlockId, String path) {
		log.trace("Getting file " + path + " in share block ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT nsf.* "
					+ "FROM SHARE_BLOCK_FILE nsf "
					+ "JOIN FILE nf ON nf.ID = nsf.FILE_ID "
					+ "WHERE nsf.SHARE_BLOCK_ID = ? "
					+ "AND nf.PATH = ?;");
			ps.setLong(1, shareBlockId);
			ps.setString(2, path);
			
			rs = ps.executeQuery();
			if (!rs.next()) {
				log.warn("File " + path + " was not found in block " + shareBlockId);
				return null;
			}
			return toShareBlockFile(rs);
			
		} catch (Exception e) {
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
	
	// Get a list of email addresses that the file share has been sent to
	public static List<ShareBlockRecipient> getShareBlockRecipients(long shareBlockId) {
		log.trace("Getting the recipient list for a share block ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT ID, EMAIL, CREATE_DATE, LAST_UPDATE_DATE "
					+ "FROM SHARE_BLOCK_RECIP "
					+ "WHERE SHARE_BLOCK_ID = ?;");
			ps.setLong(1, shareBlockId);
			
			rs = ps.executeQuery();
			return toShareBlockRecipientList(rs);
			
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
	
	// Increment the visit count for a  file share
	public static boolean incrementShareBlockVisitCount(long shareBlockId) {
		log.trace("Incrementing visits for share block ID " + shareBlockId);
		
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		PreparedStatement up = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT VISIT_COUNT FROM SHARE_BLOCK WHERE ID = ?;");
			ps.setLong(1, shareBlockId);
			rs = ps.executeQuery();
			if (!rs.next()) throw new SQLException("No share block was found with ID " + shareBlockId);
			int currCount = rs.getInt(1);
			
			up = con.prepareStatement(
					"UPDATE SHARE_BLOCK SET VISIT_COUNT = ? WHERE ID = ?;");
			up.setInt(1, ++currCount);
			up.setLong(2, shareBlockId);
			int upd = up.executeUpdate();
			if (upd != 1) throw new SQLException("Increment of VISIT_COUNT failed for SHARE_BLOCK with ID " + shareBlockId); 
			return true;
		} catch (SQLException e) {
			log.error(e, e);
			return false;
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
				if (rs != null) rs.close();
				if (up != null) up.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	// Check to see if a given  share token already exists in the database
	public static boolean tokenExists(String token) {
		log.trace("Checking if share block token " + token + " already exists");
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT 1 FROM SHARE_BLOCK WHERE TOKEN = ?;");
			ps.setString(1, token);
			rs = ps.executeQuery();
			if (rs.next()) {
				log.debug("Token exists");
				return true;
			}
			log.trace("Token does not exist");
			return false;
		} catch (SQLException e) {
			log.error(e, e);
			return true; // Play it safe
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
	
	// Check to see if a share block with a given name already exists for a user
	public static boolean shareNameExists(long userId, String name, Long ignoredShareBlockId) {
		log.trace("Checking if share name " + name + " exists for user ID " + userId 
				+ (ignoredShareBlockId != null ? ". Ignoring SHARE_BLOCK.ID=" + ignoredShareBlockId : ""));
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT 1 FROM SHARE_BLOCK WHERE USER_ID = ? AND NAME = ?" +
					(ignoredShareBlockId != null ? " AND ID != ?;" : ";"));
			ps.setLong(1, userId);
			ps.setString(2, name);
			if (ignoredShareBlockId != null) ps.setLong(3, ignoredShareBlockId);
			rs = ps.executeQuery();
			if (rs.next()) {
				log.debug("Name exists");
				return true;
			}
			log.trace("Name does not exist");
			return false;
		} catch (SQLException e) {
			log.error(e, e);
			return true; // Play it safe
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
	
	// Create a new  file share in the database
	public static boolean insertShareBlock(ShareBlock share) {
		log.trace("Creating share block "+ share);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK "
					+ "(USER_ID, IS_EXTERNAL, ALLOW_EXT_UPLOAD, TOKEN, NAME, MSG, VISIT_COUNT, EXP_DATE, PW_DIGEST, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, ?, ?, ?, 0, ?, ?, SYSDATE, SYSDATE);");
			int idx = 0;
			ps.setLong(++idx, share.getUserId());
			ps.setBoolean(++idx, share.isExternal());
			ps.setBoolean(++idx, share.isExternalUploadAllowed());
			ps.setString(++idx, share.getToken());
			ps.setString(++idx, share.getName());
			ps.setString(++idx, share.getMessage());
			if (share.getExpirationDate() == null) 
				ps.setNull(++idx, Types.DATE);
			else 
				ps.setDate(++idx, new java.sql.Date(share.getExpirationDate().getTime()));
			ps.setString(++idx, share.getPasswordDigest());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of SHARE_BLOCK record failed");
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
	
	// Create the recipients for a given  file share in the database
	public static boolean insertShareBlockRecipients(List<ShareBlockRecipient> recipients) {
		log.trace("Inserting " + recipients.size() + " share block recipients");
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK_RECIP "
					+ "(SHARE_BLOCK_ID, EMAIL, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, SYSDATE, SYSDATE);");
			
			
			for (ShareBlockRecipient rcp: recipients) {
				ps.setLong(1, rcp.getShareBlockId());
				ps.setString(2, rcp.getEmail());
				if (ps.executeUpdate() != 1) {
					if (ps != null) ps.close();
					con.close();
					throw new SQLException("Insert of SHARE_BLOCK_RECIPIENT record failed");
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
	
	public static boolean insertShareBlockRecipient(ShareBlockRecipient recipient) {
		log.trace("Inserting " + recipient);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK_RECIP "
					+ "(SHARE_BLOCK_ID, EMAIL, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, SYSDATE, SYSDATE);");
			
			ps.setLong(1, recipient.getShareBlockId());
			ps.setString(2, recipient.getEmail());
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of SHARE_BLOCK_RECIPIENT record failed");
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
	
	public static boolean updateShareBlockRecipient(ShareBlockRecipient recipient) {
		log.trace("Updating " + recipient);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE SHARE_BLOCK_RECIP "
					+ "SET EMAIL=?, "
					+ "LAST_UPDATE_DATE=SYSDATE "
					+ "WHERE ID=?;");
			
			ps.setString(1, recipient.getEmail());
			ps.setLong(2, recipient.getId());
			if (ps.executeUpdate() != 1) throw new SQLException("Update of SHARE_BLOCK_RECIPIENT record failed");
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

	// Create the file contents for a given  file share in the database
	public static boolean insertShareBlockFiles(List<ShareBlockFile> sharedFiles) {
		log.trace("Creating " + sharedFiles.size() + " share block files");
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK_FILE "
					+ "(SHARE_BLOCK_ID, FILE_ID, NOTE, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, SYSDATE, SYSDATE);");
			
			for (ShareBlockFile sf: sharedFiles) {
				if (sf.getShareBlockId() == null) {
					con.close();
					if (ps != null) ps.close();
					throw new SQLException("ShareBlock ID cannot be null");
				}
				if (sf.getNimbusFileId() == null) {
					con.close();
					if (ps != null) ps.close(); 
					throw new SQLException("NimbusFile ID cannot be null");
				}
				
				ps.setLong(1, sf.getShareBlockId());
				ps.setLong(2, sf.getNimbusFileId());
				ps.setString(3, sf.getNote());
				if (ps.executeUpdate() != 1) {
					if (ps != null) ps.close();
					con.close();
					throw new SQLException("Insert of SHARE_BLOCK_FILE record failed");
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
	
	public static boolean deleteShareBlockFile(long shareBlockFileId) {
		log.trace("Deleting ShareBlockFile ID " + shareBlockFileId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM SHARE_BLOCK_FILE WHERE ID = ?;");
			
			ps.setLong(1, shareBlockFileId);
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of SHARE_BLOCK_FILE record failed");
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
	
	public static boolean insertShareBlockFile(ShareBlockFile sharedFile) {
		log.trace("Creating " + sharedFile);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK_FILE "
					+ "(SHARE_BLOCK_ID, FILE_ID, NOTE, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, SYSDATE, SYSDATE);");
			
			if (sharedFile.getShareBlockId() == null) throw new SQLException("ShareBlock ID cannot be null");
			if (sharedFile.getNimbusFileId() == null) throw new SQLException("NimbusFile ID cannot be null");
			
			ps.setLong(1, sharedFile.getShareBlockId());
			ps.setLong(2, sharedFile.getNimbusFileId());
			ps.setString(3, sharedFile.getNote());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of SHARE_BLOCK_FILE record failed");
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
	
	public static boolean updateShareBlock(ShareBlock share) {
		log.trace("Updating share block " + share);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			
			// Update
			ps = con.prepareStatement(
					"UPDATE SHARE_BLOCK "
					+ "SET USER_ID = ?, "
					+ "TOKEN = ?, "
					+ "NAME = ?, "
					+ "MSG = ?, "
					+ "EXP_DATE = ?, "
					+ "PW_DIGEST = ?, "
					+ "IS_EXTERNAL = ?, "
					+ "ALLOW_EXT_UPLOAD = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			ps.setLong(1, share.getUserId());
			ps.setString(2, share.getToken());
			ps.setString(3, share.getName());
			ps.setString(4, share.getMessage());
			if (share.getExpirationDate() == null) 
				ps.setNull(5, Types.DATE);
			else 
				ps.setDate(5, new java.sql.Date(share.getExpirationDate().getTime()));
			ps.setString(6, share.getPasswordDigest());
			ps.setBoolean(7, share.isExternal());
			ps.setBoolean(8, share.isExternalUploadAllowed());
			ps.setLong(9, share.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("There was an error updating the share block");
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
	
	// Update the file contents of a file share
	public static boolean updateShareBlockFileContents(long shareBlockId, List<ShareBlockFile> contents) {
		log.trace("Updating contents of share block with ID " + shareBlockId);
		
		Connection con = null;
		
		String sbfIds = "";
		for (ShareBlockFile sbf : contents) {
			if (!sbf.getShareBlockId().equals(shareBlockId)) 
				throw new IllegalArgumentException("All ShareBlockFiles must have the same ShareBlock ID");
			sbfIds += sbf.getId() != null ? sbf.getId() + ", " : "";
		}
		sbfIds = !sbfIds.isEmpty() ? sbfIds.substring(0, sbfIds.length() - 2) : ""; // trim last comma
		
		try {
			con = HikariConnectionPool.getConnection();
			con.prepareStatement(
					"DELETE FROM SHARE_BLOCK_FILE WHERE SHARE_BLOCK_ID=" + shareBlockId 
					+ (!sbfIds.isEmpty() ? " AND ID NOT IN(" + sbfIds + ")" : "")
					+ ";").executeUpdate();
			
			for (ShareBlockFile sbf : contents) {
				if (sbf.getId() != null) {
					if (!updateShareBlockFile(sbf)) {
						con.close();
						throw new SQLException("Update of SHARE_BLOCK_FILE record failed.");
					}
				} else {
					if (!insertShareBlockFile(sbf)) {
						con.close();
						throw new SQLException("Insert of SHARE_BLOCK_FILE record failed.");
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
	
	// Update an individual SharedFile
	public static boolean updateShareBlockFile(ShareBlockFile sf) {
		log.trace("Updating share block file" + sf);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE SHARE_BLOCK_FILE SET NOTE = ?, LAST_UPDATE_DATE = SYSDATE WHERE ID = ?;");
			ps.setString(1, sf.getNote());
			ps.setLong(2, sf.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of SHARE_BLOCK_FILE record failed");
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
	
	// Update the list of emails that a share has been sent to
	public static boolean updateShareBlockRecipients(long shareBlockId, List<ShareBlockRecipient> recipients) {
		log.trace("Updating recipients for share block with ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		String sbrIds = "";
		for (ShareBlockRecipient sbr : recipients) {
			if (!sbr.getShareBlockId().equals(shareBlockId)) 
				throw new IllegalArgumentException("All ShareBlockRecipients must have the same ShareBlock ID");
			sbrIds += sbr.getId() != null ? sbr.getId() + ", " : "";
		}
		sbrIds = !sbrIds.isEmpty() ? sbrIds.substring(0, sbrIds.length() - 2) : ""; // trim last comma
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM SHARE_BLOCK_RECIP WHERE SHARE_BLOCK_ID=" + shareBlockId 
					+ (!sbrIds.isEmpty() ? " AND ID NOT IN(" + sbrIds + ")" : "")
					+ ";");
			ps.executeUpdate();
			
			for (ShareBlockRecipient sbr : recipients) {
				if (sbr.getId() != null) {
					if (!updateShareBlockRecipient(sbr)) {
						con.close();
						ps.close();
						throw new SQLException("Update of SHARE_BLOCK_RECIP record failed.");
					}
				} else {
					if (!insertShareBlockRecipient(sbr)) {
						con.close();
						ps.close();
						throw new SQLException("Insert of SHARE_BLOCK_RECIP record failed.");
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
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static boolean deleteShareBlock(long shareBlockId) {
		log.trace("Deleting share block ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement("DELETE FROM SHARE_BLOCK WHERE ID = ?;");
			
			ps.setLong(1, shareBlockId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Deletion of SHARE_BLOCK record failed");
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
	
	public static List<ShareBlockAccess> getShareBlockAccess(long shareBlockId) {
		log.trace("Retrieving block access for ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT ID, SHARE_BLOCK_ID, USER_ID, CAN_CREATE, CAN_UPDATE, CAN_DELETE, CREATE_DATE, LAST_UPDATE_DATE "
					+ "FROM SHARE_BLOCK_ACCESS WHERE SHARE_BLOCK_ID = ?;");
			ps.setLong(1, shareBlockId);
			
			return toShareBlockAccessList(ps.executeQuery());
		} catch (SQLException e) {
			log.error(e, e);
			return Collections.emptyList();
		} finally {
			try {
				if (con != null) con.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	public static ShareBlockAccess getShareBlockAccess(long shareBlockId, long userId) {
		log.debug("Retrieving share block access for block ID " + shareBlockId + " and user ID " + userId);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT ID, SHARE_BLOCK_ID, USER_ID, CAN_CREATE, CAN_UPDATE, CAN_DELETE, CREATE_DATE, LAST_UPDATE_DATE "
					+ "FROM SHARE_BLOCK_ACCESS WHERE SHARE_BLOCK_ID = ? AND USER_ID = ?;");
			ps.setLong(1, shareBlockId);
			ps.setLong(2, userId);
			rs = ps.executeQuery();
			
			if (!rs.next()) return null;
			return toShareBlockAccess(rs);
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
	
	public static boolean insertShareBlockAccess(ShareBlockAccess shareAccess) {
		log.debug("Creating share block access: "+ shareAccess);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO SHARE_BLOCK_ACCESS "
					+ "(SHARE_BLOCK_ID, USER_ID, CAN_CREATE, CAN_UPDATE, CAN_DELETE, CREATE_DATE, LAST_UPDATE_DATE) "
					+ "VALUES "
					+ "(?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			ps.setLong(1, shareAccess.getShareBlockId());
			ps.setLong(2, shareAccess.getUserId());
			ps.setBoolean(3, shareAccess.canCreate());
			ps.setBoolean(4, shareAccess.canUpdate());
			ps.setBoolean(5, shareAccess.canDelete());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Insert of SHARE_BLOCK_ACCESS record failed");
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
	
	public static boolean updateShareBlockAccess(ShareBlockAccess shareBlockAccess) {
		log.trace("Updating share block access: " + shareBlockAccess);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			
			// Update
			ps = con.prepareStatement(
					"UPDATE SHARE_BLOCK_ACCESS "
					+ "SET SHARE_BLOCK_ID = ?, "
					+ "USER_ID = ?, "
					+ "CAN_CREATE = ?, "
					+ "CAN_UPDATE = ?, "
					+ "CAN_DELETE = ?, "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			ps.setLong(1, shareBlockAccess.getShareBlockId());
			ps.setLong(2, shareBlockAccess.getUserId());
			ps.setBoolean(3, shareBlockAccess.canCreate());
			ps.setBoolean(4, shareBlockAccess.canUpdate());
			ps.setBoolean(5, shareBlockAccess.canDelete());
			ps.setLong(6, shareBlockAccess.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Update of SHARE_BLOCK_ACCESS record failed");
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
	
	public static boolean deleteShareBlockAccess(long shareBlockAccessId) {
		log.debug("Deleting share block access ID "+ shareBlockAccessId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM SHARE_BLOCK_ACCESS WHERE ID=?;");
			ps.setLong(1, shareBlockAccessId);
			
			if (ps.executeUpdate() != 1) throw new SQLException("Delete of SHARE_BLOCK_ACCESS record failed");
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
	
	public static boolean updateShareBlockAccess(long shareBlockId, List<ShareBlockAccess> access) {
		log.trace("Updating access for share block with ID " + shareBlockId);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		String sbaIds = "";
		for (ShareBlockAccess sba : access) {
			if (!sba.getShareBlockId().equals(shareBlockId)) 
				throw new IllegalArgumentException("All ShareBlockAccess objects must have the same ShareBlock ID");
			sbaIds += sba.getId() != null ? sba.getId() + ", " : "";
		}
		sbaIds = !sbaIds.isEmpty() ? sbaIds.substring(0, sbaIds.length() - 2) : ""; // trim last comma
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM SHARE_BLOCK_ACCESS WHERE SHARE_BLOCK_ID=" + shareBlockId 
					+ (!sbaIds.isEmpty() ? " AND ID NOT IN(" + sbaIds + ")" : "")
					+ ";");
			ps.executeUpdate();
			
			for (ShareBlockAccess sba : access) {
				if (sba.getId() != null) {
					if (!updateShareBlockAccess(sba)) {
						con.close();
						ps.close();
						throw new SQLException("Update of SHARE_BLOCK_ACCESS record failed.");
					}
				} else {
					if (!insertShareBlockAccess(sba)) {
						con.close();
						ps.close();
						throw new SQLException("Insert of SHARE_BLOCK_ACCESS record failed.");
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
				if (ps != null) ps.close();
			} catch (SQLException e) {
				log.error(e, e);
			}
		}
	}
	
	static List<ShareBlock> toShareBlockList(ResultSet rs) {
		List<ShareBlock> result = new ArrayList<ShareBlock>();
		try {
			while (rs.next()) 
				result.add(toShareBlock(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static ShareBlock toShareBlock(ResultSet row) {
		try {
			long id = row.getLong("ID");
			Boolean external = row.getBoolean("IS_EXTERNAL");
			Boolean upload = row.getBoolean("ALLOW_EXT_UPLOAD");
			String token = row.getString("TOKEN");
			long userId = row.getLong("USER_ID");
			String name = row.getString("NAME");
			String msg = row.getString("MSG");
			Integer visitCount = row.getInt("VISIT_COUNT");
			Date expires = row.getTimestamp("EXP_DATE");
			String pw = row.getString("PW_DIGEST");
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new ShareBlock(id, userId, external, upload, token, name, msg, visitCount, expires, pw, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
	
	static List<ShareBlockFile> toShareBlockFileList(ResultSet rs) {
		List<ShareBlockFile> result = new ArrayList<ShareBlockFile>();
		try {
			while (rs.next()) 
				result.add(toShareBlockFile(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static ShareBlockFile toShareBlockFile(ResultSet row) {
		try {
			Long id = row.getLong("ID");
			Long shareId = row.getLong("SHARE_BLOCK_ID");
			Long fileId = row.getLong("FILE_ID");
			String note = row.getString("NOTE");
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new ShareBlockFile(id, fileId, shareId, note, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
	
	static List<ShareBlockRecipient> toShareBlockRecipientList(ResultSet rs) {
		List<ShareBlockRecipient> result = new ArrayList<ShareBlockRecipient>();
		try {
			while (rs.next()) 
				result.add(toShareBlockRecipient(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static ShareBlockRecipient toShareBlockRecipient(ResultSet row) {
		try {
			Long id = row.getLong("ID");
			Long shareId = row.getLong("SHARE_BLOCK_ID");
			String email = row.getString("EMAIL");
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new ShareBlockRecipient(id, shareId, email, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
	
	static List<ShareBlockAccess> toShareBlockAccessList(ResultSet rs) {
		List<ShareBlockAccess> result = new ArrayList<ShareBlockAccess>();
		try {
			while (rs.next()) 
				result.add(toShareBlockAccess(rs));
		} catch (SQLException e) {
			log.error(e, e);
		}
		return result;
	}
	
	static ShareBlockAccess toShareBlockAccess(ResultSet row) {
		try {
			Long id = row.getLong("ID");
			Long shareId = row.getLong("SHARE_BLOCK_ID");
			Long userId = row.getLong("USER_ID");
			Boolean c = row.getBoolean("CAN_CREATE");
			Boolean u = row.getBoolean("CAN_UPDATE");
			Boolean d = row.getBoolean("CAN_DELETE");
			Date created = row.getTimestamp("CREATE_DATE");
			Date updated = row.getTimestamp("LAST_UPDATE_DATE");
			
			return new ShareBlockAccess(id, shareId, userId, c, u, d, created, updated);
		} catch (SQLException e) {
			log.error(e, e);
		}
		return null;
	}
}