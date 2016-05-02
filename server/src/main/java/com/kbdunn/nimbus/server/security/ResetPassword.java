package com.kbdunn.nimbus.server.security;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;

import com.kbdunn.nimbus.common.model.NimbusUser;
import com.kbdunn.nimbus.server.jdbc.JdbcHelper;

public class ResetPassword {

	public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
		setLogLevel();
		outln("Welcome to the Nimbus password reset utility!");
		outln("Please wait while we connect to Nimbus...");
		
		Connection con = null;
		try {
			con = JdbcHelper.createConnection();//HikariConnectionPool.getConnection();
		} catch (SQLException e) {
			outln("Error connecting to the database!");
			outln("Be sure Nimbus is stopped before running this utility.");
			System.exit(1);
		}
		
		NimbusUser user = promptUserSelect(con);
		String pw = promptNewPassword();
		outln("Resetting " + user.getName() + "'s password....");
		user.setPasswordDigest(PasswordHash.createHash(pw));
		
		if (!update(user, con)) outln("There was an error resetting the password!");
		else outln("Done!");
	}
	
	private static NimbusUser promptUserSelect(Connection con) {
		NimbusUser selected = null;
		
		while (selected == null) {
			List<NimbusUser> admins = getAdmins(con);
			
			if (admins.isEmpty()) {
				outln("No users have been created. Start Nimbus to create one.");
				System.exit(1);
			}

			outln("Select an admin user to reset:");
			
			for (int i=0; i < admins.size(); i++)
				outln("  " + (i+1) + ". " + admins.get(i).getName() + " (" + admins.get(i).getEmail() + ")");
			
			out("Enter your selection [1-" + admins.size() + "]: ");
			Integer n = numeric(readln());
			if (n == null) continue;
			if (n > 0 && n <= admins.size()) selected = admins.get(n-1);
			else outln("Woah! Enter a valid number, please.");
		}
		
		return selected;
	}
	
	private static String promptNewPassword() {
		String pw = null;
		String pwpw = null;
		
		while (true) {
			out("Enter the new password: ");
			pw = readPassword();
			out("Re-enter the new password: ");
			pwpw = readPassword();
			
			if (pw.equals(pwpw)) return pw;
			else outln("Woah! Those passwords don't match.");
		}
	}
	
	private static void out(String out) {
		System.out.print(out);
	}
	
	private static void outln(String out) {
		System.out.println(out);
	}
	
	private static String readln() {
		String line = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			line = br.readLine();
		} catch (IOException ioe) {
			outln("Woah! Try that again.");
		}
		return line;
	}
	
	private static String readPassword() {
		Console c = System.console();
		if (c == null) {
			return readln();
		}
		return new String(c.readPassword());
	}
	
	private static Integer numeric(String s) {
		try {
			return Integer.valueOf(s);
		} catch (NumberFormatException e) {
			outln("Woah! Enter a number, please.");
			return null;
		}
	}
	
	private static void setLogLevel() {
		LogManager.getLogger("org.hsqldb").setLevel(Level.OFF);
		LogManager.getLogger("com.kbdunn").setLevel(Level.OFF);
		LogManager.getRootLogger().setLevel(Level.OFF);
	}
	
	private static List<NimbusUser> getAdmins(Connection con) {
		List<NimbusUser> admins = new ArrayList<NimbusUser>();
		
		ResultSet rs = null;
		
		try {
			rs = con.prepareStatement("SELECT ID, NAME, EMAIL FROM NIMBUS.USER WHERE IS_ADMIN;").executeQuery();
			
			while (rs.next()) {
				NimbusUser user = new NimbusUser();
				user.setId(rs.getLong("ID"));
				user.setName(rs.getNString("NAME"));
				user.setEmail(rs.getNString("EMAIL"));
				admins.add(user);
			}
			return admins;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return admins;
	}
	
	private static boolean update(NimbusUser user, Connection con) {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement("UPDATE NIMBUS.USER SET PW_DIGEST=? WHERE ID=?;");
			ps.setString(1, user.getPasswordDigest());
			ps.setLong(2, user.getId());
			
			if (ps.executeUpdate() != 1) return false;
			con.prepareStatement("COMMIT").execute();
			con.prepareStatement("DISCONNECT").execute();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				ps.close();
				con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
}