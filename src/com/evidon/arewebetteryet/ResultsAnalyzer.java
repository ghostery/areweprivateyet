package com.evidon.arewebetteryet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.collect.Ordering;

public class ResultsAnalyzer {
	// VM prop: -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");
	Map<String, Integer> requestCountPerDomain = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> requestCountPerDomainMinusFirstParties = null;
	Map<String, Integer> setCookieResponses = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> cookiesAdded = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> cookiesDeleted = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());

	int totalContentLength = 0;

	private void createForwardHostColumnForLocalStorage(Statement statement) throws Exception {
		Map<String, String> hostMap = new HashMap<String, String>();
		
		statement.execute("ALTER TABLE local_storage ADD host TEXT");

		ResultSet rs = statement.executeQuery("select id, scope from local_storage");
		while (rs.next()) {
			String host = new StringBuilder(rs.getString("scope")).reverse().toString();
			host = host.replaceAll(":https?:[0-9][0-9][0-9]?", "");

			hostMap.put(rs.getString("id"), host);
		}
		rs.close();
		
		PreparedStatement updateQuery = statement.getConnection().prepareStatement("UPDATE local_storage SET host = ? WHERE id = ?");
		
		for (String id : hostMap.keySet()) {
			updateQuery.setString(1, hostMap.get(id));
			updateQuery.setString(2, id);
			
			updateQuery.executeUpdate();
		}
		
		updateQuery.close();
	}


	public ResultsAnalyzer(String dbFileName) throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path + dbFileName);

		Statement statement = conn.createStatement();

		createForwardHostColumnForLocalStorage(statement);

		// Request Count
		ResultSet rs = statement.executeQuery("select * from http_requests where url is not null");
		while (rs.next()) {
			String domain = "";
			
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("Cant parse this domain: " + rs.getString("url"));
				//e.printStackTrace();
				continue;
			}

			try {
				if (requestCountPerDomain.containsKey(domain)) {
					// increase hit count
					Integer count = requestCountPerDomain.get(domain);
					requestCountPerDomain.put(domain, new Integer(count.intValue() + 1));
				} else {
					// insert new domain and initial count
					requestCountPerDomain.put(domain, new Integer(1));
				}
			} catch (java.lang.IllegalArgumentException e) {
				requestCountPerDomain.put(domain, new Integer(1));
			}
		}
		rs.close();
		
		// Request count minus first parties
		requestCountPerDomainMinusFirstParties = new LinkedHashMap<String, Integer>(requestCountPerDomain);

		BufferedReader in = new BufferedReader(new FileReader(path + "top500.list"));
	    String line = in.readLine();
	    while (line != null) {
	    	try {	    	
	    		if (requestCountPerDomainMinusFirstParties.containsKey(line)) {
	    			requestCountPerDomainMinusFirstParties.remove(line);
	    		}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    	
	        line = in.readLine();
	    }
	    in.close();
		
		// Set cookie response counts
		rs = statement.executeQuery(
				"select hr.url, htr.name, htr.value from http_response_headers htr, http_responses hr " +
				"where htr.name = 'Set-Cookie' and htr.http_response_id = hr.id and hr.url is not null");
		while(rs.next()) {
			String domain = "";
			
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("Cant parse this domain: " + rs.getString("url"));
				//e.printStackTrace();
				continue;
			}

			try {
				if (setCookieResponses.containsKey(domain)) {
					// increase hit count
					Integer count = setCookieResponses.get(domain);
					setCookieResponses.put(domain, new Integer(count.intValue() + 1));
				} else {
					// insert new domain and initial count
					setCookieResponses.put(domain, new Integer(1));
				}
			} catch (java.lang.IllegalArgumentException e) {
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
				try {
					if (cookiesAdded.containsKey(domain)) {
						// increase hit count
						Integer count = cookiesAdded.get(domain);
						cookiesAdded.put(domain, new Integer(count.intValue() + 1));
					} else {
						// insert new domain and initial count
						cookiesAdded.put(domain, new Integer(1));
					}
				} catch (java.lang.IllegalArgumentException e) {
					cookiesAdded.put(domain, new Integer(1));
				}
			} else if (rs.getString("change").equals("deleted")) {
				try {
					if (cookiesDeleted.containsKey(domain)) {
						// increase hit count
						Integer count = cookiesDeleted.get(domain);
						cookiesDeleted.put(domain, new Integer(count.intValue() + 1));
					} else {
						// insert new domain and initial count
						cookiesDeleted.put(domain, new Integer(1));
					}
				} catch (java.lang.IllegalArgumentException e) {
					cookiesAdded.put(domain, new Integer(1));
				}
			}
		}
		rs.close();
		
		
		// total content length
		rs = statement.executeQuery("select value from http_response_headers where name = 'Content-Length'");
		while(rs.next()) {
			totalContentLength += rs.getInt("value");
		}
		rs.close();
		
		conn.close();
	}

	public static void main(String[] args) {
		try {
			ResultsAnalyzer ra = new ResultsAnalyzer("baseline-fourthparty.sqlite");
			System.out.println(ra.totalContentLength);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
