package com.kbdunn.nimbus.server.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.kbdunn.nimbus.common.server.PropertiesService;
import com.kbdunn.nimbus.server.NimbusContext;

public abstract class JdbcHelper {
	
	// Gets the columns contained in a ResultSet
	public static List<String> getResultColumns(ResultSet rs) {
		List<String> columns = new ArrayList<String>();
		try {
			ResultSetMetaData rsmd = rs.getMetaData();
			for (int i = 1; i <= rsmd.getColumnCount(); i++)
				columns.add(rsmd.getColumnLabel(i));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return columns;
	}
	
	// This explicitly makes a new connection
	public static Connection createConnection() throws SQLException {
		final PropertiesService props = NimbusContext.instance().getPropertiesService();
		try {
			// Load database driver if not already loaded
			Class.forName(props.getDbDriver());
			// Establish network connection to database
			Connection connection = DriverManager.getConnection(
					props.getDbConnectString(), 
					props.getDbUser(),
					props.getDbPassword()
				);
			return (connection);
		} catch (ClassNotFoundException cnfe) {
			// Simplify try/catch blocks of people using this by throwing only
			// one exception type.
			throw new SQLException("Can't find class for driver: " + props.getDbDriver());
		}
	}
}