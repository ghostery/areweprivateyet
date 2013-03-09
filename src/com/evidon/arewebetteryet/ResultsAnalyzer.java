package com.evidon.arewebetteryet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class ResultsAnalyzer {
	String path = "C:/Users/fixanoid/workspace/arewebetteryet/src/";
	Map<String, Integer> requestCountPerDomain = new HashMap<String, Integer>();
	Map<String, Integer> setCookieResponses = new HashMap<String, Integer>();
	Map<String, Integer> cookiesAdded = new HashMap<String, Integer>();
	Map<String, Integer> cookiesDeleted = new HashMap<String, Integer>();

	public ResultsAnalyzer(String dbFileName) throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path + dbFileName);

		Statement statement = conn.createStatement();
		
		// Request Count
		ResultSet rs = statement.executeQuery("select * from http_requests");
		while(rs.next()) {
			String domain = AnalysisUtils.getBaseDomain(rs.getString("url"));

			if (requestCountPerDomain.containsKey(domain)) {
				// increase hit count
				Integer count = requestCountPerDomain.get(domain);
				requestCountPerDomain.put(domain, new Integer(count.intValue() + 1));
			} else {
				// insert new domain and initial count
				requestCountPerDomain.put(domain, new Integer(1));
			}
		}
		rs.close();
		
		// Set cookie response counts
		rs = statement.executeQuery(
				"select hr.url, htr.name, htr.value from http_response_headers htr, http_responses hr " +
				"where htr.name = 'Set-Cookie' and htr.http_response_id = hr.id");
		while(rs.next()) {
			String domain = AnalysisUtils.getBaseDomain(rs.getString("url"));

			if (setCookieResponses.containsKey(domain)) {
				// increase hit count
				Integer count = setCookieResponses.get(domain);
				setCookieResponses.put(domain, new Integer(count.intValue() + 1));
			} else {
				// insert new domain and initial count
				setCookieResponses.put(domain, new Integer(1));
			}
		}
		rs.close();
		
		// Cookies Added/Deleted
		rs = statement.executeQuery(
				"select * from cookies");
		while(rs.next()) {
			String domain = rs.getString("host");

			if (rs.getString("change").equals("added")) {
				if (cookiesAdded.containsKey(domain)) {
					// increase hit count
					Integer count = cookiesAdded.get(domain);
					cookiesAdded.put(domain, new Integer(count.intValue() + 1));
				} else {
					// insert new domain and initial count
					cookiesAdded.put(domain, new Integer(1));
				}	
			} else if (rs.getString("change").equals("deleted")) {
				if (cookiesDeleted.containsKey(domain)) {
					// increase hit count
					Integer count = cookiesDeleted.get(domain);
					cookiesDeleted.put(domain, new Integer(count.intValue() + 1));
				} else {
					// insert new domain and initial count
					cookiesDeleted.put(domain, new Integer(1));
				}	
			}
		}
		rs.close();
		
		conn.close();
/*
		//requestCountPerDomain = AnalysisUtils.sortByValue(requestCountPerDomain);
		System.out.println("Request count per domain");
		for (String domain : requestCountPerDomain.keySet()) {
			System.out.println(domain + "|" + requestCountPerDomain.get(domain).intValue());
		}

		System.out.println();
		System.out.println();
		System.out.println("Set-Cookie response count per domain");
		//setCookieResponses = AnalysisUtils.sortByValue(setCookieResponses);

		for (String domain : setCookieResponses.keySet()) {
			System.out.println(domain + "|" + setCookieResponses.get(domain).intValue());
		}
*/
	}

	public static void main(String[] args) {
		try {
			new ResultsAnalyzer("baseline-fourthparty.sqlite");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
