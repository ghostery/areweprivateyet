package com.evidon.arewebetteryet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class Crawler {
	String path = "C:\\Users\\fixanoid\\workspace\\arewebetteryet\\src\\";
	ArrayList<String> urls = new ArrayList<String>();
	
	private void loadSiteList() throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(path + "top500.list"));
	    String line = in.readLine();
	    while (line != null) {
	        urls.add(line);
	        line = in.readLine();
	    }
	    in.close();
	}

	private FirefoxProfile manageProfile(String xpi) throws Exception {
		FirefoxProfile firefoxProfile = new FirefoxProfile();
		firefoxProfile.addExtension(new File(path + "extensions/" + "fourthparty.xpi"));

		if (xpi != null) {
			firefoxProfile.addExtension(new File(path + "extensions/" + xpi));
		}

		return firefoxProfile;
	}

	private String getDriverProfile() {
		File sysTemp = new File(System.getProperty("java.io.tmpdir"));
		File pd = null;
		long prevTime = 0;

		for (File t : sysTemp.listFiles()) {
			if (!t.isDirectory()) { continue; }
			
			if (t.toString().contains("webdriver-profile")) {
				if (prevTime == 0) {
					prevTime = System.currentTimeMillis() - t.lastModified();
				}

				if (prevTime >= System.currentTimeMillis() - t.lastModified()) {
					pd = t;
					prevTime = System.currentTimeMillis() - t.lastModified();
				}
			}
		}
			
		return pd.toString();
	}
	
	/*
	 1. Setup Firefox instance
	 2. Crawl domains
	 3. Collect info
	 	- cookies
	 		- totals
	 		- amount set per domain or per tracker
	 	- flash cookies
	 	- local storage
	 	- requests
	 		- totals
	 	- redirects
	 		- totals
	 	- amount of data transfer
	 */

	public Crawler(String xpi) throws Exception {
		loadSiteList();
		
		WebDriver driver = new FirefoxDriver(manageProfile(xpi));
		
		// figure out where the fucking profile is. wow!
		String profileDir = getDriverProfile();

		Scanner sc = new Scanner(System.in);
        System.out.println("Waiting to configuration. Press enter when setup is complete.");
        
        while(sc.hasNextLine()) {
        	System.out.println("Starting crawling");
        	break;
        }
		
		for (String url : urls) {
			System.out.println("navigating to: " + url);
			driver.get("http://" + url);
			// WTF, why would their own fucking wait not work?!?
			// new WebDriverWait(driver, 5 * 1000);
			try { Thread.sleep(5 * 1000); } catch (InterruptedException e) { }
		}
		
		// copy the fourthparty database out.
		FileUtils.copyFile(new File(profileDir + "/fourthparty.sqlite"), new File(path + "/" + System.currentTimeMillis() + "-fourthparty.sqlite"));
		
		driver.quit();
	}

	public static void main(String args[]) {
		try {
			new Crawler(null);
			// with ghostery:
			// new Crawler("ghostery-amo-v2.9.2.xpi");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
