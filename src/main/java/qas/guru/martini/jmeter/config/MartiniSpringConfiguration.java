/*
Copyright 2017 Penny Rohr Curich

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package qas.guru.martini.jmeter.config;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.common.base.Splitter;

import qas.guru.martini.MartiniConstants;
import qas.guru.martini.event.DefaultAfterSuiteEvent;
import qas.guru.martini.event.DefaultBeforeSuiteEvent;

import static org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.*;
import static qas.guru.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfiguration extends ConfigTestElement implements TestStateListener, Serializable {

	private static final long serialVersionUID = 4860248616231023021L;

	protected static final String PROPERTY_CONFIG_LOCATION = "contextLocation";
	protected static final String PROPERTY_ARGUMENTS = "arguments";

	public MartiniSpringConfiguration() {
		setArguments(new Arguments());
	}

	public void setArguments(Arguments arguments) {
		JMeterProperty property = new ObjectProperty(PROPERTY_ARGUMENTS, arguments);
		setProperty(property);
	}

	public void setConfigLocation(String location) {
		this.setProperty(PROPERTY_CONFIG_LOCATION, location);
	}

	public String getConfigLocation() {
		return getPropertyAsString(PROPERTY_CONFIG_LOCATION);
	}

	public void addArgument(String name, String value) {
		Arguments arguments = getArguments();
		arguments.addArgument(name, value);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.cast(o);
	}

	public void removeArguments() {
		setArguments(new Arguments());
	}

	@Override
	public boolean expectsModification() {
		return false;
	}

	@Override
	public void testStarted() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}

		String location = this.getConfigLocation();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{location}, false);

		List<String> profiles = getActiveProfiles();
		if (!profiles.isEmpty()) {
			ConfigurableEnvironment environment = context.getEnvironment();
			String[] asArray = profiles.toArray(new String[profiles.size()]);
			environment.setActiveProfiles(asArray);
		}

		PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
		Properties properties = getProperties();
		configurer.setProperties(properties);
		configurer.setLocalOverride(true);
		configurer.setSearchSystemEnvironment(true);
		configurer.setSystemPropertiesMode(SYSTEM_PROPERTIES_MODE_FALLBACK);
		context.addBeanFactoryPostProcessor(configurer);
		context.refresh();

		variables.putObject(VARIABLE_SPRING_CONTEXT, context);

		DefaultBeforeSuiteEvent event = new DefaultBeforeSuiteEvent(System.currentTimeMillis(), threadContext);
		context.publishEvent(event);
	}

	private List<String> getActiveProfiles() {
		// TODO: this should be a separate GUI field
		Arguments arguments = getArguments();
		Map<String, String> index = arguments.getArgumentsAsMap();
		String argument = index.get(MartiniConstants.ARGUMENT_SPRING_PROFILES_ACTIVE);
		return null == argument ?
			Collections.emptyList() : Splitter.on(',').trimResults().omitEmptyStrings().splitToList(argument);
	}

	private Properties getProperties() {
		Arguments arguments = getArguments();
		Map<String, String> argumentsAsMap = arguments.getArgumentsAsMap();

		Properties properties = new Properties();
		for (Map.Entry<String, String> mapEntry : argumentsAsMap.entrySet()) {
			String key = mapEntry.getKey().trim();
			if (!key.isEmpty()) {
				String value = mapEntry.getValue().trim();
				properties.setProperty(key, value);
			}
		}
		return properties;
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testEnded() {
		JMeterContext threadContext = getThreadContext();

		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(VARIABLE_SPRING_CONTEXT);
		if (ConfigurableApplicationContext.class.isInstance(o)) {
			ConfigurableApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);

			DefaultAfterSuiteEvent event = new DefaultAfterSuiteEvent(System.currentTimeMillis(), threadContext);
			context.publishEvent(event);
			context.close();
		}
	}

	@Override
	public void testEnded(String host) {
	}
}
