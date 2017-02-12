package com.gsd.pos.manager.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.gsd.pos.dao.ConnectionProvider;
import com.gsd.pos.utils.Config;

public class MySqlConnectionProvider implements ConnectionProvider {
	private Connection conn = null;
	private static final Logger logger = Logger
			.getLogger(MySqlConnectionProvider.class.getName());

	static {
		// Load the JDBC driver
		String driverName = "org.gjt.mm.mysql.Driver";
		try {
			Class.forName(driverName);
		} catch (ClassNotFoundException cnfe) {
			System.out.println("Could not load [" + driverName + "]");
			cnfe.printStackTrace();
			;
		}
	}

	public MySqlConnectionProvider() throws SQLException {
		this.initConnection();
	}

	public Connection getConnection()  {
		return conn;
	}

	private void initConnection() throws SQLException {
		String serverName = Config.getProperty("servername");
		String mydatabase = Config.getProperty("databasename");
		String username = Config.getProperty("username");
		String password = Config.getProperty("password");
		// Create a connection to the database
		String url = "jdbc:mysql://" + serverName + "/" + mydatabase;
		logger.trace("Database URL  >>" + url);
		conn = DriverManager.getConnection(url, username, password);

	}

}
