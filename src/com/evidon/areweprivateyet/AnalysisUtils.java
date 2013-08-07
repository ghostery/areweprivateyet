// Copyright 2013 Evidon.  All rights reserved.
// Use of this source code is governed by a Apache License 2.0
// license that can be found in the LICENSE file.

package com.evidon.areweprivateyet;

import java.net.URI;

import com.google.common.net.InternetDomainName;

public class AnalysisUtils {
	/**
	 * Will take a url such as http://www.stackoverflow.com and return
	 * www.stackoverflow.com
	 * 
	 * @param url
	 * @return
	 */
	public static String getHost(String url) {
		if (url == null || url.length() == 0)
			return "";

		int doubleslash = url.indexOf("//");
		if (doubleslash == -1)
			doubleslash = 0;
		else
			doubleslash += 2;

		int end = url.indexOf('/', doubleslash);
		end = end >= 0 ? end : url.length();

		return url.substring(doubleslash, end);
	}

	public static String getGuavaDomain(String url) throws Exception {
		if (url.indexOf("#") > 0) {
			url = url.substring(0, url.indexOf("#"));
		}

		if (url.indexOf("?") > 0) {
			url = url.substring(0, url.indexOf("?"));
		}

		if (url.indexOf(";") > 0) {
			url = url.substring(0, url.indexOf(";"));
		}

		if (url.indexOf("|") > 0) {
			url = url.substring(0, url.indexOf("|"));
		}
		
		if (url.indexOf("_") > 0) {
			url = url.replaceAll("_", "");
		}

		if (url.indexOf("%") > 0) {
			url = url.replaceAll("%", "");
		}
		
		// strip port
		if (url.indexOf(":8080") > 0) {
			url = url.replaceAll(":8080", "");
		}
		
		String host = new URI(url).getHost();
		try {
			InternetDomainName domainName = InternetDomainName.from(host);
			return domainName.topPrivateDomain().name();
		} catch (java.lang.IllegalStateException e) {
			return AnalysisUtils.getBaseDomain(url);
		} catch (java.lang.IllegalArgumentException e) {
			if (url.startsWith("https://")) {
				url = url.substring(7);
			}

			if (url.startsWith("http://")) {
				url = url.substring(7);
			}
			
			if (url.indexOf("/") > 0) {
				url = url.substring(0, url.indexOf("/"));
			}
			
			if (url.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) {
				return url;
			} else {
				throw new Exception();
			}
		}
	}

	/**
	 * Based on :
	 * http://grepcode.com/file/repository.grepcode.com/java/ext/com.google
	 * .android/android/2.3
	 * .3_r1/android/webkit/CookieManager.java#CookieManager.getBaseDomain%28java.lang.String%2
	 * 9 Get the base domain for a given host or url. E.g. mail.google.com will
	 * return google.com
	 * 
	 * @param host
	 * @return
	 */
	public static String getBaseDomain(String url) {
		String host = getHost(url);

		int startIndex = 0;
		int nextIndex = host.indexOf('.');
		int lastIndex = host.lastIndexOf('.');
		while (nextIndex < lastIndex) {
			startIndex = nextIndex + 1;
			nextIndex = host.indexOf('.', startIndex);
		}
		if (startIndex > 0) {
			return host.substring(startIndex);
		} else {
			return host;
		}
	}
}
