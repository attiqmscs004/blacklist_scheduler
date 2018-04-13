package com.blacklist.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.agiserver.helper.DatabaseException;
import com.agiserver.helper.common.ConfigurationLoader;

public class DBConnectionManager {
	private static DBConnectionManager	dbMngr		= null;
	private String						cnxnString	= null;

	private DBConnectionManager() throws DatabaseException {
		this.cnxnString = (ConfigurationLoader.getProperty("db.url") + "?user=" + ConfigurationLoader.getProperty("db.user") + "&password=" + ConfigurationLoader.getProperty("db.password") + "&zeroDateTimeBehavior=convertToNull");
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception e) {
			throw new DatabaseException(e, 5, e.getMessage());
		}
	}

	public static DBConnectionManager getInstance() throws DatabaseException {
		if (dbMngr == null) {
			dbMngr = new DBConnectionManager();
		}
		return dbMngr;
	}

	public synchronized Connection getConnection() throws DatabaseException {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(this.cnxnString);
		} catch (SQLException e) {
			throw new DatabaseException(e, 10, e.getMessage());
		}
		return conn;
	}

	public Connection getConnection(String url, String user, String password) throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	public Connection getDramaConnection() throws SQLException {
		Connection conn = null;
		conn = getConnection(ConfigurationLoader.getProperty("db.url.poke"), ConfigurationLoader.getProperty("db.user.poke"), ConfigurationLoader.getProperty("db.password.poke"));
		return conn;
	}
}
