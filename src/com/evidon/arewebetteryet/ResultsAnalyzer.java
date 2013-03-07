package com.evidon.arewebetteryet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ResultsAnalyzer {
	public ResultsAnalyzer() throws Exception {
		String path = "C:/Users/fixanoid/workspace/arewebetteryet/src/";

		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path + "1362694536409-fourthparty.sqlite");

		Statement statement = conn.createStatement();
		ResultSet rs = statement.executeQuery("select * from pages");
		while(rs.next()) {
			System.out.println(rs.getString("location"));
		}

		conn.close();
	}

	public static void main(String[] args) {
		try {
			new ResultsAnalyzer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
