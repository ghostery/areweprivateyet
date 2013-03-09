package com.evidon.arewebetteryet;

public class AnalysisUtils {
	/**
	 * Will take a url such as http://www.stackoverflow.com and return www.stackoverflow.com
	 * 
	 * @param url
	 * @return
	 */
	public static String getHost(String url){
	    if(url == null || url.length() == 0)
	        return "";

	    int doubleslash = url.indexOf("//");
	    if(doubleslash == -1)
	        doubleslash = 0;
	    else
	        doubleslash += 2;

	    int end = url.indexOf('/', doubleslash);
	    end = end >= 0 ? end : url.length();

	    return url.substring(doubleslash, end);
	}


	/**  Based on : http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.3_r1/android/webkit/CookieManager.java#CookieManager.getBaseDomain%28java.lang.String%29
	 * Get the base domain for a given host or url. E.g. mail.google.com will return google.com
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
