arewebetteryet - WIP name
-------------------------

This project is dedicated to automated recreation of Stanford's CIS "Tracking the Trackers Self Help Tools" study.
Original study is located at this link: http://cyberlaw.stanford.edu/blog/2011/09/tracking-trackers-self-help-tools

This project is separated broadly into 3 areas:
- Firefox setup for testing
- fourthparty extension
- Crawler and Analysis utilities

The crawler is based on Selenium through Firefox WebDriver interface. Each run, while using an existing profile 
as a base is actually a separate anonymous profile for Firefox, and as such, the last thing crawler will do is
copy the fourthparty sqlite database into a path defined at run time.

All required libraries for the test are stored in lib directory.


Setup
=====

To set up the tool you will need Firefox (10 and up), fourthparty build, and arewebetteryet analysis utilities. 
We currently test the following extensions in addition to baseline:
- Ghostery
- DoNotTrackMe
- Adblock Plus with Fanboys list
- Adblock Plus with EasyList
- TrackerBlock
- RequestPolicy
- Disconnect
- NoScript

Firefox profiles need to be set up prior to running crawler from analysis utilities. The profiles need to be named
as follows: "ghostery", "dntme", "abp-fanboy", "abp-easylist", "trackerblock", "requestpolicy", "disconnect",
"noscript". Each profile needs to contain fourthparty install (tho this could be force installed on the profile
through Selenium within crawler) and the extension that is being tested. You are also responsible for setting up
the extension in the way that you want to test, meaning that if you just install ABP and have no lists, obviously
the result would be different if it did contain EasyPrivacy or any other list.

Note: You may add your own extensions by modifying the utilities array, or you may request that we add your extension for
future testing by emailing me at felix@evidon.com. Please remember that baseline is always the first profile to be 
executed.

Executing the crawl
===================

