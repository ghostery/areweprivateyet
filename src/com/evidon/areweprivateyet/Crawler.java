package com.evidon.areweprivateyet;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class Crawler {

	public Crawler() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		/*
		FourthParty implemented:
		 	- cookies
		 	- requests
		 	- redirects
		 	- local storage
		 	- amount of data transfer
	 	TODO:
		 	- flash cookies
		 */
        // create ExecutorService to manage threads OR empty (better)

		
		try {
			String[] profiles = {"baseline", "ghostery", "ublock", "dntme-blue", "disconnect", "abp-fanboy", "abp-easylist", "trackerblock", /*"requestpolicy", "noscript",*/ "cookies-blocked"};
//			String[] profiles = {"abp-fanboy","trackerblock", "cookies-blocked", "avgdnt"};
			int numProfiles = profiles.length;
			ArrayList<CrawlerTask> tasks = new ArrayList<CrawlerTask>();
			for (String profile : profiles) {
				Thread.sleep(10000);
				tasks.add(new CrawlerTask(profile));
			}
			
			ExecutorService es = Executors.newFixedThreadPool(numProfiles);
			int i = 0;
			for (String profile : profiles) {
				es.execute(tasks.get(i++));
//			for (String profile : profiles) {
//				new CrawlerTask(profile).run();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
