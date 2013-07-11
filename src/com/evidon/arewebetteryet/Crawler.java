package com.evidon.arewebetteryet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

public class Crawler {
	// VM prop -Dawby_path=C:/Users/fixanoid-work/Desktop/arewebetteryet/bin/
	String path = System.getProperty("awby_path");
	ArrayList<String> urls = new ArrayList<String>();
	StringBuilder out = new StringBuilder();
	
	private void recordLog(String name) throws IOException {
		BufferedWriter out = new BufferedWriter(new FileWriter(path + "crawl-" + name + ".log"));
		out.write(out.toString());
		out.close();
	}

	private void loadSiteList() throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(path + "top500.list"));
	    String line = in.readLine();
	    while (line != null) {
	        urls.add(line);
	        line = in.readLine();
	    }
	    in.close();
	}

	private String getDriverProfile() {
		// C:\Users\ADMINI~1\AppData\Local\Temp\2\
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

	private void log(String s) {
		out.append(s + "\n");
		System.out.println(s);
	}

	private void handleTimeout(String baseWindow, String url, WebDriver driver) {
		log("\tTimed out loading " + url + ", skipping.");
		killPopups(baseWindow, driver);
	}
	
	private void killPopups(String baseWindow, WebDriver driver) {
		// close any new popups.
		for (String handle : driver.getWindowHandles()) {
			if (!handle.equals(baseWindow)) {
				WebDriver popup = driver.switchTo().window(handle);
				log("\tClosing popup: " + popup.getCurrentUrl());
				popup.close();

				// TODO: need to see if this breaks when there is a modal.
			}
		}

		driver.switchTo().window(baseWindow);
	}

	public Crawler(String namedProfile) throws Exception {
		loadSiteList();

		int sleepTime = (namedProfile.equals("baseline") ? 10 : 5);
		boolean started = false;
		String baseWindow = "";

		FirefoxProfile profile = new ProfilesIni().getProfile(namedProfile);
		//profile.setPreference("webdriver.load.strategy", "fast");

		WebDriver driver = new FirefoxDriver(profile);

		driver.manage().timeouts().implicitlyWait(40, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(40, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(40, TimeUnit.SECONDS);

		// figure out where the fucking profile is. wow!
		String profileDir = getDriverProfile();
		
        log("Crawling started for " + namedProfile);
 	
        int count = 0;
		for (String url : urls) {
			if (!started) {
				// Original window handle to be used as base. Used so we can close all other popups.  
				baseWindow = driver.getWindowHandle();
				started = true;
			}

			count++;
			log("\t" + count + ". navigating to: " + url);

			CrawlusInterruptus ci = new CrawlusInterruptus(60);
			try {
				ci.start();

				try {
					// Confirm handling for one of those super fucking annoying "Are you sure you wonna go anywhere else?"
					driver.switchTo().alert().accept();
					log("\tAccepted a navigate away modal");
				} catch (Exception e) { }
				
				driver.get("http://" + url);				
				
				// WTF, why would their own fucking wait not work?!?
				// new WebDriverWait(driver, 5 * 1000);
			} catch (TimeoutException te) {
				handleTimeout(baseWindow, url, driver);
			} catch (org.openqa.selenium.UnhandledAlertException me) {
				log("\tModal exception caused by previous site?");

				// Retry current site.
				try {
					driver.get("http://" + url);
				} catch (TimeoutException te) {
					handleTimeout(baseWindow, url, driver);
				}
			} finally {
				ci.interrupt();
			}

			try { Thread.sleep(sleepTime * 1000); } catch (InterruptedException e) { }

			killPopups(baseWindow, driver);
		}

		// navigating to the trip site for local storage copy.
		try { driver.get("http://www.josesignanini.com"); } catch (TimeoutException te) { }
		try { Thread.sleep(60 * 1000); } catch (InterruptedException e) { }
		
		// copy the fourthparty database out.
		FileUtils.copyFile(new File(profileDir + "/fourthparty.sqlite"), new File(path + "/fourthparty-" + namedProfile + ".sqlite"));

		driver.quit();
		log("Crawling completed for " + namedProfile);
		
		recordLog(namedProfile);
	}

	public static void main(String args[]) {
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
		try {
			String[] profiles = {"baseline", "ghostery", "dntme", "disconnect", "abp-fanboy", "abp-easylist", "trackerblock", /*"requestpolicy", "noscript",*/ "cookies-blocked"};
			for (String profile : profiles) {
				new Crawler(profile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
