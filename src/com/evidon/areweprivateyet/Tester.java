package com.evidon.areweprivateyet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Tester {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Class.forName("org.sqlite.JDBC");
			Connection conn = DriverManager.getConnection("jdbc:sqlite:C:/Users/Administrator/Desktop/workspac/arewebetteryet/src/fourthparty-0.1/fourthparty-baseline.sqlite");
			Statement statement = conn.createStatement();
			
			conn.setAutoCommit(false);

			int count = 0;

			PreparedStatement updateQuery = statement.getConnection().prepareStatement(
					"UPDATE http_responses SET public_suffix=? WHERE id=? ");
			
			ResultSet rs = statement.executeQuery("SELECT id, url FROM http_responses WHERE url is not null and url !=''");
			while (rs.next()) {
				String domain = "";
				try {
					domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
				} catch (Exception e) {
					System.out.println("Cant parse this domain: " + rs.getString("url"));
					//e.printStackTrace();
					continue;
				}

				
				updateQuery.setString(1, domain);
				updateQuery.setString(2, rs.getString("id"));
				updateQuery.addBatch();

				if (count % 1000 == 0) {
					System.out.println("Batching");
					updateQuery.executeBatch();
					
					conn.commit();

					updateQuery.clearParameters();
				}
				count ++;
			}
			rs.close();
			
			updateQuery.executeBatch();
			conn.commit();

			updateQuery.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
