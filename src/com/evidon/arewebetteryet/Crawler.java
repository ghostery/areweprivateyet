package com.evidon.arewebetteryet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.internal.ProfilesIni;

public class Crawler {
	String path = "C:\\Users\\fixanoid-work\\Desktop\\arewebetteryet\\src\\";
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

	public Crawler(String namedProfile) throws Exception {
		loadSiteList();
		
		ProfilesIni profile = new ProfilesIni();
		WebDriver driver = new FirefoxDriver(profile.getProfile(namedProfile));

		driver.manage().timeouts().implicitlyWait(40, TimeUnit.SECONDS);
		driver.manage().timeouts().pageLoadTimeout(40, TimeUnit.SECONDS);
		driver.manage().timeouts().setScriptTimeout(40, TimeUnit.SECONDS);

		// figure out where the fucking profile is. wow!
		String profileDir = getDriverProfile();

        System.out.println("Crawling started.");
 	
		for (String url : urls) {
			System.out.println("navigating to: " + url);

			try {
				driver.get("http://" + url);
				// WTF, why would their own fucking wait not work?!?
				// new WebDriverWait(driver, 5 * 1000);
			} catch (TimeoutException te) {
				System.out.println("Timed out, skipping.");
			}

			try { Thread.sleep(5 * 1000); } catch (InterruptedException e) { }
		}

		// navigating to the trip site for local storage copy.
		try { driver.get("http://www.josesignanini.com"); } catch (TimeoutException te) { }
		try { Thread.sleep(60 * 1000); } catch (InterruptedException e) { }
		
		// copy the fourthparty database out.
		FileUtils.copyFile(new File(profileDir + "/fourthparty.sqlite"), new File(path + "/" + namedProfile + "-fourthparty.sqlite"));

		driver.quit();
		System.out.println("Crawling completed.");
	}

	public static void main(String args[]) {
		/*
		FourthParty collect info. Implemented:
		 	- cookies
		 	- requests
		 	- redirects
		 	- amount of data transfer
	 	TODO:
		 	- flash cookies
		 	- local storage
		 */
		try {
			String[] profiles = {"baseline", "ghostery", "dntme", "abp-fanboy", "abp-easylist", "trackerblock", "collusion", "disconnect", "noscript"};
			for (String profile : profiles) {
				new Crawler(profile);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
