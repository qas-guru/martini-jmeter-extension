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

package guru.qas.martini.jmeter.config;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import guru.qas.martini.event.DefaultMartiniSuiteIdentifier;
import guru.qas.martini.event.EventManager;
import guru.qas.martini.event.MartiniSuiteIdentifier;

import static guru.qas.martini.MartiniConstants.PROPERTY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfiguration extends ConfigTestElement
	implements NoThreadClone, TestStateListener, TestIterationListener, Serializable {

	private static final long serialVersionUID = 4860248616231023021L;
	protected static final String PROPERTY_CONFIGS = "contextLocations";
	protected static final String PROPERTY_PROFILES = "springProfiles";
	protected static final String PROPERTY_ENVIRONMENT = "environment";

	protected final transient AtomicReference<ConfigurableApplicationContext> contextRef;

	public MartiniSpringConfiguration() {
		super();
		contextRef = new AtomicReference<>();
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

	public void setEnvironmentProperties(Arguments arguments) {
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
		synchronized (contextRef) {
			ConfigurableApplicationContext context = initializeContext();
			ConfigurableApplicationContext previous = contextRef.getAndSet(context);
			if (null != previous) {
				previous.close();
			}

			String hostname = JMeterUtils.getLocalHostName();
			JMeterContext threadContext = super.getThreadContext();
			JMeterVariables variables = threadContext.getVariables();
			long timestamp = Long.valueOf(variables.get("TESTSTART.MS"));
			String name = super.getName();
			UUID id = UUID.randomUUID();

			MartiniSuiteIdentifier identifier = DefaultMartiniSuiteIdentifier.builder()
				.setHostname(hostname)
				.setTimetamp(timestamp)
				.setSuiteName(name)
				.setId(id)
				.build();

			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			beanFactory.registerSingleton("martiniSuiteIdentifier", identifier); // TODO: constant

			EventManager eventManager = context.getBean(EventManager.class);
			eventManager.publishBeforeSuite(this, identifier);
		}
	}

	protected ConfigurableApplicationContext initializeContext() {
		ConfigurableApplicationContext context = null;
		try {
			ApplicationContextBuilder contextBuilder = getContextBuilder();
			context = contextBuilder.build();
		}
		catch (Exception e) {
			String message = String.format("%s unable to start Spring", getName());
			logError(message, e);
			showError(message);
		}
		return context;
	}

	protected ApplicationContextBuilder getContextBuilder() {
		return new DefaultApplicationContextBuilder()
			.setConfigLocations(getProperty(PROPERTY_CONFIGS))
			.setProfiles(getProperty(PROPERTY_PROFILES))
			.setEnvironment(getProperty(PROPERTY_ENVIRONMENT));
	}

	protected void logError(String message, Throwable cause) {
		String implementation = getClass().getName();
		Logger logger = LoggingManager.getLoggerFor(implementation);
		logger.error(message, cause);
	}

	protected void showError(String message) {
		if (!JMeter.isNonGUI()) {
			GuiPackage.showErrorMessage(message + "; see logs for details.", "Martini Error");
		}
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		TestElement source = event.getSource();
		JMeterContext threadContext = source.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		testIterationStart(threadGroup);
	}

	protected void testIterationStart(AbstractThreadGroup threadGroup) {
		JMeterProperty property = threadGroup.getProperty(PROPERTY_SPRING_CONTEXT);
		if (NullProperty.class.isInstance(property)) {
			ApplicationContext context = contextRef.get();
			if (null == context) {
				threadGroup.stop();
			}
			else {
				property = new ObjectProperty(PROPERTY_SPRING_CONTEXT, context);
				threadGroup.setProperty(property);
				threadGroup.setTemporary(property);
			}
		}
	}

	@Override
	public void testEnded() {
		synchronized (contextRef) {
			ConfigurableApplicationContext context = contextRef.getAndSet(null);
			if (null != context) {
				publishTestEnded(context);
				context.close();
			}
		}
	}

	protected void publishTestEnded(ApplicationContext context) {
		MartiniSuiteIdentifier identifier = context.getBean(MartiniSuiteIdentifier.class);
		EventManager eventManager = context.getBean(EventManager.class);
		eventManager.publishAfterSuite(this, identifier);
	}

	@Override
	public void testEnded(String host) {
	}
}
