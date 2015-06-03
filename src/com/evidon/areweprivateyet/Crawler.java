package com.evidon.areweprivateyet;

import static org.openqa.selenium.Platform.MAC;
import static org.openqa.selenium.Platform.WINDOWS;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.internal.ProfilesIni;

import com.google.common.collect.Maps;

public class Crawler {

	public static boolean MULTI_RUN = true;
	public Crawler() {
		// TODO Auto-generated constructor stub
	}
	
	//Taken from the Celenium's ProfilesIni
	private static File newProfile(String name, File appData, String path, boolean isRelative) {
		    if (name != null && path != null) {
		      File profileDir = isRelative ? new File(appData, path) : new File(path);
		      return profileDir;
		    }
		    return null;
		  }
	
	private static Map<String, File> readProfiles(File appData) {
	    Map<String, File> toReturn = Maps.newHashMap();

	    File profilesIni = new File(appData, "profiles.ini");
	    if (!profilesIni.exists()) {
	      // Fine. No profiles.ini file
	      return toReturn;
	    }

	    boolean isRelative = true;
	    String name = null;
	    String path = null;

	    BufferedReader reader = null;
	    try {
	      reader = new BufferedReader(new FileReader(profilesIni));

	      String line = reader.readLine();

	      while (line != null) {
	        if (line.startsWith("[Profile")) {
	          File profile = newProfile(name, appData, path, isRelative);
	          if (profile != null)
	            toReturn.put(name, profile);

	          name = null;
	          path = null;
	        } else if (line.startsWith("Name=")) {
	          name = line.substring("Name=".length());
	        } else if (line.startsWith("IsRelative=")) {
	          isRelative = line.endsWith("1");
	        } else if (line.startsWith("Path=")) {
	          path = line.substring("Path=".length());
	        }

	        line = reader.readLine();
	      }
	    } catch (IOException e) {
	      throw new WebDriverException(e);
	    } finally {
	      try {
	        if (reader != null) {
	          File profile = newProfile(name, appData, path, isRelative);
	          if (profile != null)
	            toReturn.put(name, profile);

	          reader.close();
	        }
	      } catch (IOException e) {
			System.out.println("failed to collect Firefox profiles");
	      }
	    }

	    return toReturn;
	}
	
	private static void Clear(String[] profileNames) {
		
		//Clear fourthparty.sqlite files in profiles
	    File appData = new File(MessageFormat.format("{0}\\Mozilla\\Firefox", System.getenv("APPDATA")));

		Map<String, File> profilesMap = readProfiles(appData);
		for (String profileName : profileNames) {
			if(profilesMap.containsKey(profileName)) {
				File profileDir = profilesMap.get(profileName);
				File fourthParty = new File(profileDir.getPath() + "\\fourthparty.sqlite");
				if(fourthParty.exists() && !fourthParty.isDirectory()) {
					try {
						fourthParty.delete();
					} 
					catch(Exception e) {
						System.out.println("failed to remove file " + fourthParty.toString());
					}
					//Ensure that there is a file in the profile directory which name is the same as the name 
					//of the profile. It helps to reliably find Celenium's driver profile directory later. 
					File marker = new File(profileDir.getPath() + "\\" + profileName);
					if(!marker.exists()) {
						try {
							marker.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							System.out.println("failed to create marker file " + marker.toString());
						}
					}
				}
			}
		}
		
		/**
		 * The temporary profile Selenium/Firefox creates should be 
		 * located at java.io.tmpdir. On Windows it is %TEMP%
		 * Here we remove remnants of the previous run.
		 */
		File sysTemp = new File(System.getProperty("java.io.tmpdir"));		
		boolean bFound = false;
		for (File t : sysTemp.listFiles()) {
			if (!t.isDirectory()) { continue; }
			
			try {
				if ((t.toString().contains("anonymous") && t.toString().contains("webdriver-profile")) || 
						(t.toString().contains("userprofile") && t.toString().contains("copy"))) {
					FileUtils.forceDelete(t);
				}
			}
			catch(Exception e) {
				System.out.println("failed to remove file " + t.toString());
			}
		}
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
			
			//String[] profiles = {"baseline", "ghostery", "dntme-blue", "ublock", "badger", "abe-suggested", "abp-suggested", "dntme-blue", "disconnect", "abp-fanboy", "abp-easylist", "trackerblock", /*"requestpolicy", "noscript",*/ "cookies-blocked"};
			String[] profiles = {"baseline", "cookies-blocked", "dnt"};
			Clear(profiles);
			
			int numProfiles = profiles.length;
			if(MULTI_RUN) { 				
				ExecutorService es = Executors.newFixedThreadPool(numProfiles);
				for (String profile : profiles) {
					es.execute(new CrawlerTask(profile));
				}
			}
			else {
				for (String profile : profiles) {
					new CrawlerTask(profile).run();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
