package com.kbdunn.nimbus.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.kbdunn.nimbus.common.model.Message;
import com.kbdunn.nimbus.common.model.ShareBlock;
import com.kbdunn.nimbus.common.model.ShareBlockComment;
import com.kbdunn.nimbus.server.jdbc.HikariConnectionPool;

public abstract class MessageDAO {
	
	private static final Logger log = LogManager.getLogger(MessageDAO.class.getName());

	public static List<ShareBlockComment> getFileShareMessages(ShareBlock share) {
		log.debug("Getting messages for file share " + share);
		
		List<ShareBlockComment> messages = new ArrayList<ShareBlockComment>();
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT * FROM MSG " +
					"WHERE SHARE_ID = ?;");
			ps.setLong(1, share.getId());
			rs = ps.executeQuery();

			while (rs.next()) {
				messages.add(toFileShareMessage(rs));
			}
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
		
		log.debug(messages.size() + " messages found");
		return messages;
	}
	
	private static Long getMessageId(Message message) {
		log.debug("Getting message " + message);
		
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"SELECT ID FROM MSG " 
					+ "WHERE SHARE_ID = ? "
					+ "AND USER_ID = ? "
					+ "AND IS_ANON = ? "
					+ "AND IP = ? "
					+ "AND SUBJECT = ? "
					+ "AND MESSAGE = ?;");
			if (message instanceof ShareBlockComment) {
				ShareBlockComment fsm = (ShareBlockComment) message;
				ps.setLong(1, fsm.getShareBlockId());
				if (fsm.getUserId() != null)
					ps.setLong(2, fsm.getUserId());
				else
					ps.setNull(2, Types.INTEGER);
				ps.setBoolean(3, fsm.getAnonymousUserIpAddress() != null);
				ps.setString(4, fsm.getAnonymousUserIpAddress());
				ps.setNull(5, Types.VARCHAR);
			}
			ps.setString(16, message.getMessage());
			rs = ps.executeQuery();
			
			if (!rs.next()) throw new SQLException("Message not found!");
			return rs.getLong(1);
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
	
	public static Long insert(Message message) {
		log.debug("Inserting message " + message);
		
		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"INSERT INTO MSG (SHARE_ID, USER_ID, PARENT_MSG_ID, IS_ANON, IP, SUBJECT, MESSAGE, CREATE_DATE, LAST_UPDATE_DATE) VALUES "
					+ "(?, ?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE);");
			
			if (message instanceof ShareBlockComment) {
				ShareBlockComment fsm = (ShareBlockComment) message;
				ps.setLong(1, fsm.getShareBlockId());
				if (fsm.getUserId() != null)
					ps.setLong(2, fsm.getUserId());
				else
					ps.setNull(2, Types.INTEGER);
				ps.setBoolean(3, fsm.getAnonymousUserIpAddress() != null);
				ps.setString(4, fsm.getAnonymousUserIpAddress());
			}
			ps.setString(5, message.getMessage());
			
			if (ps.executeUpdate() != 1) throw new SQLException("No records inserted!");
			
			return getMessageId(message);
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
		return null;
	}

	public static void update(Message message) {
		log.debug("Updating message " + message);

		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"UPDATE MSG SET "
					+ "SHARE_ID = ? "
					+ "USER_ID = ? "
					+ "IS_ANON = ? "
					+ "IP = ? "
					+ "MESSAGE = ? "
					+ "LAST_UPDATE_DATE = SYSDATE "
					+ "WHERE ID = ?;");
			if (message instanceof ShareBlockComment) {
				ShareBlockComment fsm = (ShareBlockComment) message;
				ps.setLong(1, fsm.getShareBlockId());
				if (fsm.getUserId() != null)
					ps.setLong(2, fsm.getUserId());
				else
					ps.setNull(2, Types.INTEGER);
				ps.setBoolean(3, fsm.getAnonymousUserIpAddress() != null);
				ps.setString(4, fsm.getAnonymousUserIpAddress());
			}
			ps.setString(5, message.getMessage());
			
			ps.setLong(6, message.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("No records updated!");
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

	public static void delete(Message message) {
		log.debug("Deleting message " + message);

		Connection con = null;
		PreparedStatement ps = null;
		
		try {
			con = HikariConnectionPool.getConnection();
			ps = con.prepareStatement(
					"DELETE FROM MSG WHERE ID = ? ");
			ps.setLong(6, message.getId());
			
			if (ps.executeUpdate() != 1) throw new SQLException("Record was not deleted!");
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
	
	private static ShareBlockComment toFileShareMessage(ResultSet row) {
		try {
			return new ShareBlockComment(row.getLong("ID"), row.getLong("USER_ID"), row.getString("MESSAGE"), row.getLong("SHARE_BLOCK_ID"), 
					row.getBoolean("IS_ANON"), row.getString("IP"), row.getTimestamp("CREATE_DATE"), row.getTimestamp("LAST_UPDATE_DATE"));
		} catch (SQLException e) {
			log.error(e, e);
			return null;
		}
	}
}
