package com.evidon.arewebetteryet;

import java.util.ArrayList;
import java.util.HashMap;

public class ResultsAggregator {
	HashMap<String, ResultsAnalyzer> results = new HashMap<String, ResultsAnalyzer>();
	
	public ResultsAggregator() { }
	
	public void addResults(String name, String dbFileName) {
		try {
			ResultsAnalyzer ra = new ResultsAnalyzer(dbFileName);
			results.put(name, ra);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// TODO: this is unfinished, plus we'll probably want csv output.
	// domain|count1|count2|countN
	public void printResultTable() { 
		HashMap<String, String> out = new HashMap<String, String>();
		ArrayList<String> domains = new ArrayList<String>();

		// create a merged list of domains.
		for (String database : results.keySet()) {
			ResultsAnalyzer ra = results.get(database);
			
			for (String domain : ra.requestCountPerDomain.keySet()) {
				if (!domains.contains(domain)) {
					domains.add(domain);
					out.put(domain, "");
				}
			}
		}
		
		// for each domain, produce an output string
		boolean first = true;
		for (String domain : domains) {
			for (String database : results.keySet()) {
				ResultsAnalyzer ra = results.get(database);
				
				if (ra.requestCountPerDomain.containsKey(domain)) {
					String count = out.get(domain);
					count += (first ? ra.requestCountPerDomain.get(domain) : "|" + ra.requestCountPerDomain.get(domain));
					out.put(domain, count);
				} else {
					String count = out.get(domain);
					count += (first ? "0" : "|0");
					out.put(domain, count);
				}
				
				if (first) {
					first = false;
				}
			}
			
			first = true;
		}
		
		// print the results
		// header
		first = true;
		for (String database : results.keySet()) {
			System.out.print( (first ? database : "|" + database) );
			
			if (first) {
				first = false;
			}
		}
		
		System.out.println();
		// results
		for (String domain : out.keySet()) {
			System.out.println( domain + "|" + out.get(domain));
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ResultsAggregator agg = new ResultsAggregator();
		agg.addResults("Baseline", "baseline-fourthparty.sqlite");
		agg.addResults("Ghostery", "ghostery-fourthparty.sqlite");
		
		agg.printResultTable();
	}

}
