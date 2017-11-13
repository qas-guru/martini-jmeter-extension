1. Obtain jmeter jar, e.g. martini-jmeter-1.1.1.jar
2. Obtain jmeter dependencies zip, e.g. martini-jmeter-1.1.1-runtime-dependencies.zip
3. Create directory to hold runtime dependencies in e.g. $JMETER_HOME/lib/martini
4. Unzip plan dependencies to directory created in previous step
5. Launch jmeter, create named but empty plan. Save to [project].jmx file.
6. Copy $JMETER_HOME/jmeter.properties to [project].properties.
7. Add martini-jmeter.jar to search_path, e.g. search_paths=/my/path/to/martini-jmeter-1.1.1.jar
8. Add martini jmeter dependencies and project dependencies to user.classpath, e.g user.classpath=../lib/martini:/my/path/to/project/dependency.jar
9. Launch jmeter with options, e.g. ./jmeter.sh -p myProject.properties -t myProject.jmx
10. Configure a Martini Spring PreProcessor at the top of your test plan. Make sure to include classpath*:/martiniJmeterContext.xml directly in the configLocations arguments or indirectly through your project configuration.
11. Create a Thread Group and optional controllers.
12. Configure a Martini Scenario PreProcessor at the highest looping level.
13. Configure a Martini Sampler at the same level or lower than the PreProcessor.
14. Execute with a results listener. If configured correctly, your suite should run to success.

