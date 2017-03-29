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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.JMeter;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.util.NoConfigMerge;
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
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.event.MartiniEvent;
import guru.qas.martini.event.MartiniEventPublisher;
import qas.guru.martini.event.DefaultAfterSuiteEvent;
import qas.guru.martini.event.DefaultBeforeSuiteEvent;

import static qas.guru.martini.MartiniConstants.PROPERTY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfiguration extends ConfigTestElement
	implements NoThreadClone, NoConfigMerge, TestStateListener, TestIterationListener, Serializable {

	private static final long serialVersionUID = 4860248616231023021L;
	protected static final String PROPERTY_CONFIGS = "contextLocations";
	protected static final String PROPERTY_PROFILES = "springProfiles";
	protected static final String PROPERTY_ENVIRONMENT = "environment";

	protected transient volatile Monitor monitor;
	protected transient volatile AtomicBoolean contextInitialized;
	protected transient volatile AtomicReference<ConfigurableApplicationContext> contextRef;

	public MartiniSpringConfiguration() {
		super();
		monitor = new Monitor();
		contextInitialized = new AtomicBoolean(false);
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
		monitor.enter();
		try {
			if (contextInitialized.compareAndSet(false, true)) {
				ConfigurableApplicationContext context = initializeContext();
				contextRef.set(context);
				publishTestStarted(context);
			}
		}
		finally {
			monitor.leave();
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

	protected void publishTestStarted(ApplicationContext context) {
		JMeterContext threadContext = getThreadContext();
		DefaultBeforeSuiteEvent event = new DefaultBeforeSuiteEvent(System.currentTimeMillis(), threadContext);
		publish(context, event);
	}

	protected void publish(ApplicationContext context, MartiniEvent event) {
		MartiniEventPublisher publisher = context.getBean(MartiniEventPublisher.class);
		publisher.publish(event);
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		monitor.enter();
		try {
			TestElement source = event.getSource();
			JMeterContext threadContext = source.getThreadContext();
			AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
			testIterationStart(threadGroup);
		}
		finally {
			monitor.leave();
		}
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
		monitor.enter();
		try {
			if (contextInitialized.compareAndSet(true, false)) {
				ConfigurableApplicationContext context = contextRef.getAndSet(null);
				if (null != context) {
					publishTestEnded(context);
					context.close();
				}
			}
		}
		finally {
			monitor.leave();
		}
	}

	protected void publishTestEnded(ApplicationContext context) {
		JMeterContext threadContext = getThreadContext();
		long now = System.currentTimeMillis();
		DefaultAfterSuiteEvent event = new DefaultAfterSuiteEvent(now, threadContext);
		publish(context, event);
	}

	@Override
	public void testEnded(String host) {
	}
}
