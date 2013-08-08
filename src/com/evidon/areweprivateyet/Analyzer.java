// Copyright 2013 Evidon.  All rights reserved.
// Use of this source code is governed by a Apache License 2.0
// license that can be found in the LICENSE file.

package com.evidon.areweprivateyet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Ordering;

public class Analyzer {
	// VM prop: -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");

	Map<String, Integer> requestCountPerDomain = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	//Map<String, Integer> requestCountPerDomainMinusFirstParties = null;
	Map<String, Integer> setCookieResponses = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> cookiesAdded = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> cookiesDeleted = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> cookieTotals = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());
	Map<String, Integer> localStorageContents = new ValueComparableMap<String, Integer>(Ordering.natural().reverse());

	int totalContentLength = 0;
	

	private void createPublicSuffix(Statement statement) throws Exception {
		try {
			statement.execute("ALTER TABLE pages ADD public_suffix TEXT");
		} catch (Exception e) {
			// Column exists.
		}

		statement.getConnection().setAutoCommit(false);

		int count = 0;
		PreparedStatement updateQuery = statement.getConnection().prepareStatement(
				"UPDATE pages SET public_suffix=? WHERE id=?");
		
		ResultSet rs = statement.executeQuery("SELECT id, location FROM pages WHERE location is not null and location !='' ");
		while (rs.next()) {
			String domain = "";
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("location"));
			} catch (Exception e) {
				//System.out.println("Cant parse this domain: " + rs.getString("location"));
				//e.printStackTrace();
				continue;
			}

			updateQuery.setString(1, domain);
			updateQuery.setString(2, rs.getString("id"));
			updateQuery.addBatch();
			
			if (count % 1000 == 0) {
				updateQuery.executeBatch();
				
				statement.getConnection().commit();

				updateQuery.clearParameters();
			}
			count ++;
		}
		rs.close();
		
		updateQuery.executeBatch();
		statement.getConnection().commit();
		updateQuery.close();

		try {
			statement.execute("ALTER TABLE http_requests ADD public_suffix TEXT");
		} catch (Exception e) {
			// Column exists.
		}
		statement.getConnection().commit();
		count = 0;
		updateQuery = statement.getConnection().prepareStatement(
				"UPDATE http_requests SET public_suffix=? WHERE id=?");
		
		rs = statement.executeQuery("SELECT id, url FROM http_requests WHERE url is not null and url !='' ");
		while (rs.next()) {
			String domain = "";
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("\tCant parse this domain: " + rs.getString("url"));
				//e.printStackTrace();
				continue;
			}

			updateQuery.setString(1, domain);
			updateQuery.setString(2, rs.getString("id"));
			updateQuery.addBatch();
			
			if (count % 1000 == 0) {
				updateQuery.executeBatch();
				
				statement.getConnection().commit();

				updateQuery.clearParameters();
			}
			count++;
		}
		rs.close();
		
		updateQuery.executeBatch();
		statement.getConnection().commit();
		updateQuery.close();
		
		try {
			statement.execute("ALTER TABLE http_responses ADD public_suffix TEXT");
		} catch (Exception e) {
			// Column exists.
		}
		statement.getConnection().commit();
		count = 0;

		updateQuery = statement.getConnection().prepareStatement(
				"UPDATE http_responses SET public_suffix=? WHERE id=? ");
		
		rs = statement.executeQuery("SELECT id, url FROM http_responses WHERE url is not null and url !=''");
		while (rs.next()) {
			String domain = "";
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("\tCant parse this domain: " + rs.getString("url"));
				//e.printStackTrace();
				continue;
			}

			updateQuery.setString(1, domain);
			updateQuery.setString(2, rs.getString("id"));
			updateQuery.addBatch();
			
			if (count % 1000 == 0) {
				updateQuery.executeBatch();
				
				statement.getConnection().commit();

				updateQuery.clearParameters();
			}
			count++;
		}
		rs.close();
		updateQuery.executeBatch();
		statement.getConnection().commit();

		updateQuery.close();
		
		statement.getConnection().setAutoCommit(true);
	}

	private void createTopPages(Statement statement) throws Exception {
		List<String> pageIds = new LinkedList<String>();
		List<String> parentIds = new LinkedList<String>();
		Map<String, String> topPages = new HashMap<String, String>();
		

		ResultSet rs = statement.executeQuery("SELECT id, parent_id FROM pages");
		while (rs.next()) {
			pageIds.add(rs.getString("id"));
			parentIds.add(rs.getString("parent_id"));
		}
		rs.close();

		String lastPage = "", lastParent = "";
		for (String pageId : pageIds) {
			lastPage = pageId;
			lastParent = parentIds.get(pageIds.indexOf(pageId));
			try {
				//System.out.println(lastPage + "|" + lastParent + "|" + parentIds.get(pageIds.indexOf(lastParent)));
				if (lastPage.equals(lastParent)) {
					topPages.put(pageId, lastParent);
				} else {
					while(!lastPage.equals(lastParent)) {
						//System.out.println(lastPage + "|" + lastParent);
						lastPage = pageIds.get(pageIds.indexOf(lastParent));
						lastParent = parentIds.get(pageIds.indexOf(lastParent));
					}
					topPages.put(pageId, lastParent);
				}
			} catch (Exception e) {}
		}
		
		try {
			statement.execute("ALTER TABLE pages ADD top_id INTEGER");
		} catch (Exception e) {
			// Columns exist.
		}
		
		statement.getConnection().setAutoCommit(false);
		int count = 0;
		PreparedStatement updateQuery = statement.getConnection().prepareStatement(
				"UPDATE pages SET top_id=? WHERE id=?");
		
		for (String id : topPages.keySet()) {
			updateQuery.setString(1, topPages.get(id));
			updateQuery.setString(2, id);
			updateQuery.addBatch();
			
			if (count % 1000 == 0) {
				updateQuery.executeBatch();
				statement.getConnection().commit();
			}
			count++;
		}
		
		updateQuery.executeBatch();
		statement.getConnection().commit();
		updateQuery.close();
		
		statement.getConnection().setAutoCommit(true);
	}

	private void createCleanedUpRedirectHosts(Statement statement) throws Exception {
		try {
			statement.execute("ALTER TABLE redirects ADD from_host TEXT");
			statement.execute("ALTER TABLE redirects ADD to_host TEXT");
			statement.execute("ALTER TABLE redirects ADD parent_host TEXT");
		} catch (Exception e) {
			// Columns exist.
		}

		PreparedStatement updateQuery = statement.getConnection().prepareStatement(
				"UPDATE redirects SET from_host = ?, to_host = ?, parent_host = ? WHERE id = ?");

		ResultSet rs = statement.executeQuery("select * from redirects");
		while (rs.next()) {
			updateQuery.setString(1, AnalysisUtils.getGuavaDomain(rs.getString("from_channel")));
			updateQuery.setString(2, AnalysisUtils.getGuavaDomain(rs.getString("to_channel")));
			try {
				updateQuery.setString(3, AnalysisUtils.getGuavaDomain(rs.getString("parent_location")));
			} catch (Exception e) {
				updateQuery.setString(3, rs.getString("parent_location"));
			}
			updateQuery.setString(4, rs.getString("id"));
			
			updateQuery.executeUpdate();
		}
		rs.close();
		
		updateQuery.close();
	}
	
	private void createForwardHostColumnForLocalStorage(Statement statement) throws Exception {
		Map<String, String> hostMap = new HashMap<String, String>();
		
		try {
			statement.execute("ALTER TABLE local_storage ADD host TEXT");
		} catch (Exception e) {
			// Column exists.
		}

		ResultSet rs = statement.executeQuery("select id, scope from local_storage");
		while (rs.next()) {
			// TODO: we should handle or remove about: and other internal pages
			String host = rs.getString("scope");
			// TODO: might want to anchor to the end of the string only
			host = host.replaceAll(":https?:[0-9][0-9][0-9]?", "");
			host = new StringBuilder(host).reverse().toString();
			
			if (host.startsWith(".")) {
				host = host.substring(1);
			}
			
			if (host.startsWith("www.")) {
				host = host.substring(4);
			}

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


	public Analyzer(String dbFileName) throws Exception {
		Class.forName("org.sqlite.JDBC");
		Connection conn = DriverManager.getConnection("jdbc:sqlite:" + path + dbFileName);

		Statement statement = conn.createStatement();

		System.out.println("\tCreating public suffixes");
		createPublicSuffix(statement);
		
		System.out.println("\tCreating top ids");
		createTopPages(statement);
		
		System.out.println("\tCreating hosts for local storage");
		createForwardHostColumnForLocalStorage(statement);
		// Uncomment once new crawling run has been made
		// createCleanedUpRedirectHosts(statement);

		// Request Count
		ResultSet rs = statement.executeQuery(
				// grab all http_requests from deeply nested frames (frames with another frame for parent)
				// TODO: missing first level frame?
				"select hr.* from "+
				"pages p, http_requests hr "+
				"where p.parent_id != p.top_id and hr.page_id = p.id "+
				// TODO: "where p.id != p.top_id and hr.page_id = p.id "+ returns more pages including the intermediaries?

				"union all "+

				// grab all http_requests from top level document where requests domain is not the same as pages domain (3rd party requests)

				"select hr.* from "+ 
				"pages p, http_requests hr "+
				"where p.id = p.top_id and hr.page_id = p.id and hr.public_suffix != p.public_suffix "+ 
				"and hr.public_suffix not in ( "+

					// try to exclude domains that are only linked to a single site
					// same query as the one above only limited to third-party requests that show up on a single page
					"select public_suffix from ( "+
						"select hr.public_suffix, count( distinct p.public_suffix) count from "+ 
						"pages p, http_requests hr "+
						"where p.id = p.top_id and hr.page_id = p.id and hr.public_suffix != p.public_suffix "+
						"group by hr.public_suffix "+
						"having count = 1 )" +
					" cdns)");
		while (rs.next()) {
			String domain = "";
			
			try {
				// TODO: whats wrong with public suffix on http_requests?
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("\tCant parse this domain: " + rs.getString("url"));
				//e.printStackTrace();
				continue;
			}

			// Count by domain so we can review the domain list for false positives. AWPY chart only uses overall number
			// TODO: I think it may be possible to just group on public suffixes in the query
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

		
		// TODO we should be ignoring opt-out cookies set by privacy extensions
		// Set cookie response counts
		rs = statement.executeQuery(
				"select hr.url, htr.name, htr.value from  " +
				"pages p, http_requests hr, http_response_headers htr " +
				"where p.parent_id != p.top_id and hr.page_id = p.id  " +
				"and htr.name = 'Set-Cookie' and htr.http_response_id = hr.id and hr.url is not null " +
				
				"union all " +
				
				"select  hr.url, htr.name, htr.value from  " +
				"pages p, http_requests hr, http_response_headers htr " +
				"where p.id = p.top_id and hr.page_id = p.id and hr.public_suffix != p.public_suffix  " +
				"and htr.name = 'Set-Cookie' and htr.http_response_id = hr.id and hr.url is not null " +
				"and hr.public_suffix not in ( " +
				
				"select public_suffix from ( " +
				"select hr.id,hr.url,hr.method,hr.referrer,hr.page_id,hr.public_suffix,count( distinct p.public_suffix) count from  " +
				"pages p, http_requests hr " +
				"where p.id = p.top_id and hr.page_id = p.id and hr.public_suffix != p.public_suffix " +
				"group by hr.public_suffix " +
				"having count = 1 ) cdns)");
		while(rs.next()) {
			String domain = "";
			
			try {
				domain = AnalysisUtils.getGuavaDomain(rs.getString("url"));
			} catch (Exception e) {
				System.out.println("\tCant parse this domain: " + rs.getString("url"));
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
			String domain = rs.getString("raw_host");

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
					cookiesDeleted.put(domain, new Integer(1));
				}
			}
		}
		rs.close();
		
		// Subtract deleted from added to achieve cookie totals
		//		Note: May be possible just to use the above query set to do the same.
		for (String domain : cookiesAdded.keySet()) {
			int cookieNum = cookiesAdded.get(domain);
			
			try {
				if (cookiesDeleted.containsKey(domain)) {
					cookieNum = cookieNum - cookiesDeleted.get(domain);
				}
			} catch (java.lang.IllegalArgumentException e) {
				// nothing in the deleted map.
			}
			
			cookieTotals.put(domain, new Integer(cookieNum));
		}
		
		
		// Local Storage
		rs = statement.executeQuery(
				"select * from local_storage");
		while(rs.next()) {
			String domain = rs.getString("host");

			try {
				if (localStorageContents.containsKey(domain)) {
					// increase hit count
					Integer count = localStorageContents.get(domain);
					localStorageContents.put(domain, new Integer(count.intValue() + 1));
				} else {
					// insert new domain and initial count
					localStorageContents.put(domain, new Integer(1));
				}
			} catch (java.lang.IllegalArgumentException e) {
				localStorageContents.put(domain, new Integer(1));
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
}
