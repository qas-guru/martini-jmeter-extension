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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopTestNowException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import qas.guru.martini.event.DefaultAfterSuiteEvent;
import qas.guru.martini.event.DefaultBeforeSuiteEvent;

import static qas.guru.martini.MartiniConstants.*;


@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfiguration extends ConfigTestElement implements TestStateListener, Serializable {

	private static final long serialVersionUID = 4860248616231023021L;

	protected static final String PROPERTY_CONFIGS = "contextLocations";
	protected static final String PROPERTY_PROFILES = "springProfiles";
	protected static final String PROPERTY_ENVIRONMENT = "environment";

	protected final org.apache.log.Logger logger;

	public MartiniSpringConfiguration() {
		super();
		logger = LoggingManager.getLoggerFor(getClass().getName());
	}

	public void setContextLocations(String location) {
		setProperty(PROPERTY_CONFIGS, location);
	}

	public String getContextLocations() {
		return getPropertyAsString(PROPERTY_CONFIGS);
	}

	public void setProfiles(String profiles) {
		setProperty(PROPERTY_PROFILES, profiles);
	}

	public String getProfiles() {
		return getPropertyAsString(PROPERTY_PROFILES);
	}

	public void setEnvironment(Arguments arguments) {
		JMeterProperty property = new ObjectProperty(PROPERTY_ENVIRONMENT, arguments);
		setProperty(property);
	}

	public Arguments getEnvironmentProperties() {
		JMeterProperty property = getProperty(PROPERTY_ENVIRONMENT);
		Object o = property.getObjectValue();
		return null == o ? null : Arguments.class.cast(o);
	}

	@Override
	public void testStarted() {
		ApplicationContext context = getApplicationContext();
		JMeterVariables variables = getVariables();
		variables.putObject(VARIABLE_SPRING_CONTEXT, context);
		publishTestStarted(context);
	}

	protected ApplicationContext getApplicationContext() {
		try {
			return new DefaultApplicationContextBuilder()
				.setConfigLocations(getProperty(PROPERTY_CONFIGS))
				.setProfiles(getProperty(PROPERTY_PROFILES))
				.setEnvironment(getProperty(PROPERTY_ENVIRONMENT))
				.build();
		}
		catch (Exception e) {
			logger.error("unable to start Spring", e);
			String message = String.format("Unable to start Spring: %s.  See log for more details.", e.getMessage());
			GuiPackage.showErrorMessage(message, "Spring Error");
			throw new JMeterStopTestNowException("unable to start Spring");
		}
	}

	protected void publishTestStarted(ApplicationContext context) {
		JMeterContext threadContext = getThreadContext();
		DefaultBeforeSuiteEvent event = new DefaultBeforeSuiteEvent(System.currentTimeMillis(), threadContext);
		context.publishEvent(event);
	}

	protected JMeterVariables getVariables() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}
		return variables;
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testEnded() {
		JMeterVariables variables = getVariables();
		Object o = variables.remove(VARIABLE_SPRING_CONTEXT);
		if (null != o) {
			ConfigurableApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);

			JMeterContext threadContext = getThreadContext();
			DefaultAfterSuiteEvent event = new DefaultAfterSuiteEvent(System.currentTimeMillis(), threadContext);
			context.publishEvent(event);
			context.close();
		}
	}

	@Override
	public void testEnded(String host) {
	}
}
