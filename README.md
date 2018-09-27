# Martini JMeter Extension

## Table of Contents
1. [What is the Martini JMeter Extension?](#what)
1. [How does the Martini JMeter Extension work?](#how)
	1. [How do I run my suite using the JMeter Martini Extension?](#how-execute)
1. [Where can I find more information?](#info)

### What is the Martini JMeter Extension Standalone? <a name="what"></a>

The Martini JMeter Extension is used to execute [Martini](https://github.com/qas-guru/martini-core) 
scenarios in a JMeter environment.

### How does the Martini Extension work? <a name="how"></a>

The extension can be included into JMeter's classpath by one of:

* placing the jar and its dependencies in ${JMETER_HOME}/lib/ext path
* editing ${JMETER_HOME}/bin/jmeter.properties, adding the extension .jar to property search_paths and 
adding the extension dependencies plus your suite .jar files to property user.classpath
* editing ${JMETER_HOME}/bin/user.properties, adding the extension .jar to property search_paths and 
adding the extension dependencies plus your suite .jar files to property user.classpath
* copying and editing your own version of jmeter.properties, adding the extension .jar to property 
search_paths and adding the extension dependencies plus your suite .jar files to property 
user.classpath then executing jmeter with a -p option pointing to your custom properties file

The test plan (.jmx file) must contain one active Martini Spring PreProcessor at the test plan level.

Execution of existing scenarios will require the addition of a Martini Controller to a thread group
with one Martini Sampler child. 

When the test initializes, the preprocessor loads the Martini suite and the controller fetches 
requested scenarios from Martini.

For each loop the controller executes, each thread is assigned one scenario until the scenarios are 
exhausted. For example, if executing with a JMeter Thread Group of 10 threads with one loop and 30
scenarios, the end result is a total execution of 30 scenarios a single time. 


### Where can I find more information? <a name="info"></a>

#### In Progress: [Martini Standalone Wiki](https://github.com/qas-guru/martini-standalone/wiki) 
#### In Progress: [__Martini - swank software testing in Java__](https://leanpub.com/martini) 