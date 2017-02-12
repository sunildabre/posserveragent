package com.gsd.pos.agent.dao.impl;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.gsd.pos.dao.ConnectionProvider;
import com.gsd.pos.utils.Config;

public class SQLServerConnectionProvider implements ConnectionProvider {
	private Connection conn = null;
	private static final Logger logger = Logger.getLogger(SQLServerConnectionProvider.class
			.getName());

	static {
		// Load the JDBC driver
		String driverName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"; 
		try {
			Class.forName(driverName);
		} catch (ClassNotFoundException cnfe) {
			System.out.println("Could not load [" + driverName + "]");
			cnfe.printStackTrace();;
		}
		
	}
	
	public SQLServerConnectionProvider(String dbIp, String databaseName, String username, String password) throws SQLException {
		this.initConnection(dbIp, databaseName, username, password);
	}


	public Connection getConnection()  {
		return conn;
	}

	private void initConnection(String serverName, String mydatabase, String username, String password) throws SQLException {
		// Create a connection to the database
		String url = "jdbc:sqlserver://"+ serverName+ ";databaseName="+ mydatabase	+ ";user=" + username + ";password=" + password + ";"; 
		logger.trace("Database URL  >>" + url);
		conn = DriverManager.getConnection(url);
	}

}
