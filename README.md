areweprivateyet - WIP name
-------------------------

This project is dedicated to automated recreation of Stanford's CIS "Tracking the Trackers Self Help Tools" study.
Original study is located at this link: http://cyberlaw.stanford.edu/blog/2011/09/tracking-trackers-self-help-tools.
This project extends certain feature counts as well, such as, total bandwidth, redirects, local storage, etc.

This project is separated broadly into 3 areas:
- Firefox setup for the project
- fourthparty extension
- Crawler and Analysis utilities

The crawler is based on Selenium through Firefox WebDriver interface. Each run, while using an existing profile 
as a base is actually a separate anonymous profile for Firefox, and as such, the last thing crawler will do is
copy the fourthparty sqlite database into a path defined at run time.

All required libraries for the test are stored in lib directory.


Setup
=====

To set up the tool you will need Firefox (10 and up), fourthparty build (Ghostery fork), and arewebetteryet analysis 
utilities. We currently test the following extensions in addition to baseline:
- Ghostery
- DoNotTrackMe
- Disconnect 2
- Adblock Plus with Fanboys list
- Adblock Plus with EasyList
- TrackerBlock
- RequestPolicy
- NoScript
- Firefox with Third Party cookies disabled

Firefox profiles need to be set up prior to running crawler from analysis utilities. The profiles must be named
as follows: __"ghostery", "dntme", "abp-fanboy", "abp-easylist", "trackerblock", "requestpolicy", "disconnect",
"noscript", "cookies-blocked"__. Each profile needs to contain fourthparty install (tho this could be force-installed on the profile
through Selenium within crawler) and the extension that is being tested. You are also responsible for setting up
the extension in the way that you want to test, meaning that if you just install ABP and have no lists, obviously
the result would be different if it did contain EasyPrivacy or any other list.

Note: You may add your own extensions by modifying the utilities array, or you may request that we add your extension for
future testing by emailing me at <felix@evidon.com>. Please remember that baseline is always the first profile to be 
executed.


Executing the crawl
===================

The Crawler is a simple Selenium based utility that will use the top500.list (you may substitute it with your own) to 
load and then navigate to each website in the list. To execute you may load the project into your IDE of choice and
simply run Crawler class.  Alternatively, you may use the build provided and run it with your local installation of
java:

```
java -Dawby_path=/output_path/ -classpath "apache-mime4j-0.6.jar:lib/bsh-1.3.0.jar:lib/cglib-nodep-2.1_3.jar:lib/commons-codec-1.6.jar:lib/commons-collections-3.2.1.jar:lib/commons-exec-1.1.jar:lib/commons-io-2.2.jar:lib/commons-jxpath-1.3.jar:lib/commons-lang3-3.1.jar:lib/commons-logging-1.1.1.jar:lib/cssparser-0.9.8.jar:lib/dom4j-1.6.1.jar:lib/guava-14.0.jar:lib/hamcrest-core-1.3.jar:lib/hamcrest-library-1.3.jar:lib/htmlunit-2.11.jar:lib/htmlunit-core-js-2.11.jar:lib/httpclient-4.2.1.jar:lib/httpcore-4.2.1.jar:lib/httpmime-4.2.1.jar:lib/ini4j-0.5.2.jar:lib/jcommander-1.29.jar:lib/jetty-websocket-8.1.8.jar:lib/jna-3.4.0.jar:lib/jna-platform-3.4.0.jar:lib/json-20080701.jar:lib/junit-dep-4.11.jar:lib/log4j-1.2.13.jar:lib/nekohtml-1.9.17.jar:lib/netty-3.5.7.Final.jar:lib/operadriver-1.2.jar:lib/phantomjsdriver-1.0.1.jar:lib/poi-3.9-20121203.jar:lib/protobuf-java-2.4.1.jar:lib/sac-1.3.jar:lib/selenium-java-2.31.0.jar:lib/serializer-2.7.1.jar:lib/sqlite-jdbc-3.7.2.jar:lib/stax-api-1.0.1.jar:lib/testng-6.8.jar:lib/xalan-2.7.1.jar:lib/xercesImpl-2.10.0.jar:lib/xml-apis-1.4.01.jar:lib/xmlbeans-2.3.0.jar:."  com.evidon.arewebetteryet.Crawler
```

awby_path is the local setting for location of the top500.list file as well as the input and output folder that will 
be used.  This value is used in Crawler and Aggregator classes.

After each extension's crawl is completed, Crawler will copy fourthparty SQLite database to the output directory 
(awby_path) to be used in the Aggregator utility later on.  The file name of the copied fourthparty database is 
fourthparty-profileName.sqlite.

Crawling may be done in any order and at any time prior to the running of analysis utilites. You may also use another
automation tool to produce the fourthparty output.


Running analysis utilities
==========================

Aggregator class is designed to query and collect information from multiple fourthparty databases into a human 
readable Excel spreadsheet as well as, produce output in JSON. To run, either execute from your IDE or use the
following command:

```
java -Dawby_path=/output_path/ -classpath "apache-mime4j-0.6.jar:lib/bsh-1.3.0.jar:lib/cglib-nodep-2.1_3.jar:lib/commons-codec-1.6.jar:lib/commons-collections-3.2.1.jar:lib/commons-exec-1.1.jar:lib/commons-io-2.2.jar:lib/commons-jxpath-1.3.jar:lib/commons-lang3-3.1.jar:lib/commons-logging-1.1.1.jar:lib/cssparser-0.9.8.jar:lib/dom4j-1.6.1.jar:lib/guava-14.0.jar:lib/hamcrest-core-1.3.jar:lib/hamcrest-library-1.3.jar:lib/htmlunit-2.11.jar:lib/htmlunit-core-js-2.11.jar:lib/httpclient-4.2.1.jar:lib/httpcore-4.2.1.jar:lib/httpmime-4.2.1.jar:lib/ini4j-0.5.2.jar:lib/jcommander-1.29.jar:lib/jetty-websocket-8.1.8.jar:lib/jna-3.4.0.jar:lib/jna-platform-3.4.0.jar:lib/json-20080701.jar:lib/junit-dep-4.11.jar:lib/log4j-1.2.13.jar:lib/nekohtml-1.9.17.jar:lib/netty-3.5.7.Final.jar:lib/operadriver-1.2.jar:lib/phantomjsdriver-1.0.1.jar:lib/poi-3.9-20121203.jar:lib/protobuf-java-2.4.1.jar:lib/sac-1.3.jar:lib/selenium-java-2.31.0.jar:lib/serializer-2.7.1.jar:lib/sqlite-jdbc-3.7.2.jar:lib/stax-api-1.0.1.jar:lib/testng-6.8.jar:lib/xalan-2.7.1.jar:lib/xercesImpl-2.10.0.jar:lib/xml-apis-1.4.01.jar:lib/xmlbeans-2.3.0.jar:."  com.evidon.arewebetteryet.Aggregator
```

Using the databases in the input folder, Aggregator collects results and outputs a final file named analysis.xls.
This should be a copy of the aforementioned study.
