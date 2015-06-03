// Copyright 2013 Evidon.  All rights reserved.
// Use of this source code is governed by a Apache License 2.0
// license that can be found in the LICENSE file.

package com.evidon.areweprivateyet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;

public class CrawlerTask implements Runnable {

	private String inPath;
	private String outPath;
	private ArrayList<String> urls = new ArrayList<String>();
	private StringBuilder strBuilder = new StringBuilder();
	private String namedProfile;
	private WebDriver driver;
	private String profileDir;

	private synchronized void recordLog(String name) throws IOException {
		BufferedWriter outBuf = new BufferedWriter(new FileWriter(outPath + "crawl-" + name + ".log"));
		outBuf.write(strBuilder.toString());
		outBuf.close();
	}

	private void loadSiteList() throws Exception {
		BufferedReader in = new BufferedReader(new FileReader(inPath + "top500.list"));
	    String line = in.readLine();
	    while (line != null) {
	        urls.add(line);
	        line = in.readLine();
	    }
	    in.close();
	}

	private String getDriverProfile() {
		/**
		 * The temporary profile Selenium/Firefox creates should be 
		 * located at java.io.tmpdir. On Windows it is %TEMP%
		 * Search is based on the presence of the file which name is the same as the name of the profile.
		 * This file was manually placed in profile directory.
		 */
		File sysTemp = new File(System.getProperty("java.io.tmpdir"));		
		File pd = null;
		boolean bFound = false;
		for (File t : sysTemp.listFiles()) {
			if (!t.isDirectory()) { continue; }
			
			if (t.toString().contains("webdriver-profile")) {
				for(File f : t.listFiles()) {
					if (f.isDirectory()) { continue; }
					
					if(System.currentTimeMillis() < t.lastModified() + 10000 && 
							f.getName().equalsIgnoreCase(namedProfile)) {
						pd = t;
						bFound = true;
						break;
					}
				}
				
				if(bFound) {break;}
			}
			
		}
			
		return pd.toString();
	}

	private synchronized void log(String s) {
		strBuilder.append(s + "\n");
		System.out.println(namedProfile + ":" + s);
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
	
	public CrawlerTask() {
	}

	public CrawlerTask(String profileName) throws Exception {
		namedProfile = profileName;
		inPath = System.getProperty("awby_path");
		Format formatter = new SimpleDateFormat("YYYY-MM-dd_hh-mm-ss");
		outPath = inPath + "results\\crawl_" + formatter.format(new Date()) + "\\";
		
		File resultsDir = new File(outPath);
		if(!resultsDir.exists())	{
		    try{
		    	resultsDir.mkdir();
		    } 
		    catch(SecurityException se){
		    	log("Failed to create output directory");
		    }  			
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			synchronized(this){
				
				FirefoxProfile profile = new ProfilesIni().getProfile(namedProfile);
				
				FirefoxBinary binary = new FirefoxBinary(new File(inPath + "launchFirefox.bat"));
				driver = new FirefoxDriver(binary, profile);
				
				driver.manage().timeouts().implicitlyWait(40, TimeUnit.SECONDS);
				driver.manage().timeouts().pageLoadTimeout(40, TimeUnit.SECONDS);
				driver.manage().timeouts().setScriptTimeout(40, TimeUnit.SECONDS);
				
				profileDir = getDriverProfile();
			}
		
		    log("Crawling started for " + namedProfile + "\nprofileDir = " + profileDir);
			loadSiteList();
			
			String baseWindow = driver.getWindowHandle();
			int sleepTime = (namedProfile.equals("baseline") ? 10 : 5);
			
			int count = 0;
			
			for (String url : urls) {
				count++;
				log("\t" + count + ". navigating to: " + url);

				//_SZCrawlusInterruptus ci = new CrawlusInterruptus(60);
				try {
					//_SZci.start();

					try {
						// Confirm handling for one of those super fucking annoying "Are you sure you wonna go anywhere else?"
						driver.switchTo().alert().accept();
						log("\tAccepted a navigate away modal");
					} catch (Exception e) { }
					
					driver.manage().timeouts().pageLoadTimeout(60, TimeUnit.SECONDS);
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
					//_SZci.interrupt();
				}

				try { Thread.sleep(sleepTime * 1000); } catch (InterruptedException e) { }

				killPopups(baseWindow, driver);
			}

			// 4th party does not know when the crawl is over, so we send a trip signal by navigating to the "last" domain
			try { driver.get("http://www.josesignanini.com"); } catch (TimeoutException te) { }
			try { Thread.sleep(60 * 1000); } catch (InterruptedException e) { }
			
			// copy the fourthparty database out.
			try {
			FileUtils.copyFile(new File(profileDir + "/fourthparty.sqlite"), new File(outPath + "fourthparty-" + namedProfile + ".sqlite"));
			} catch(Exception e) {
			}

			driver.quit();
			log("Crawling completed for " + namedProfile);

			recordLog(namedProfile);
        
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
