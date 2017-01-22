package com.gsd.pos.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionProvider {
	public Connection getConnection() throws SQLException ;


}