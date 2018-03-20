/*
Copyright 2018 Penny Rohr Curich

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

package guru.qas.martini.jmeter.processor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

import guru.qas.martini.MartiniException;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.runtime.event.EventManager;

import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public final class MartiniSpringPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 6143536078921717477L;

	public static final String DEFAULT_RESOURCES_CONTEXT = "classpath*:**/contextOne.xml,classpath*:**/contextTwo.xml";
	public static final String DEFAULT_RESOURCES_FEATURES = "classpath*:**/*.feature";

	protected static final String PROPERTY_CONFIG_LOCATIONS = "martini.spring.config.locations";
	protected static final String PROPERTY_FEATURE_LOCATIONS = "martini.feature.locations";
	protected static final String PROPERTY_SYSTEM_PROPERTIES = "martini.system.properties";
	protected static final String BEAN_SUITE_IDENTIFIER = "suiteIdentifier";

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniSpringPreProcessor.class);

	public MartiniSpringPreProcessor() {
		super();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		TestElement source = event.getSource();
		JMeterContext threadContext = source.getThreadContext();
		setSpringContext(threadContext);
	}

	private void setSpringContext(JMeterContext threadContext) {
		JMeterVariables variables = threadContext.getVariables();
		ConfigurableApplicationContext springContext = getSpringContext().orElse(null);
		if (null == springContext) {
			variables.remove(KEY_SPRING_CONTEXT);
		}
		else {
			variables.putObject(KEY_SPRING_CONTEXT, springContext);
		}
	}

	public void process() {
		LOGGER.debug("in process()");
		JMeterContext threadContext = super.getThreadContext();
		ConfigurableApplicationContext springContext = getSpringContext().orElse(null);
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		if (null == springContext) {
			samplerContext.remove(KEY_SPRING_CONTEXT);
		}
		else {
			samplerContext.put(KEY_SPRING_CONTEXT, springContext);
		}
	}

	private Optional<ConfigurableApplicationContext> getSpringContext() {
		JMeterProperty property = this.getProperty(KEY_SPRING_CONTEXT);
		Object o = property.getObjectValue();
		ConfigurableApplicationContext context = ConfigurableApplicationContext.class.isInstance(o) ?
			ConfigurableApplicationContext.class.cast(o) : null;
		return Optional.ofNullable(context);
	}

	public void testStarted(String host) {
		testStarted();
	}

	public void testStarted() {
		try {
			PropertySource propertySource = getEnvironmentPropertySource();
			ConfigurableApplicationContext springContext = setUpSpring(propertySource);
			SuiteIdentifier suiteIdentifier = setUpSuiteIdentifier(springContext);

			EventManager eventManager = springContext.getBean(EventManager.class);
			eventManager.publishBeforeSuite(this, suiteIdentifier);

			setSpringProperty(springContext);
			setSpringVariable(springContext);
		}
		catch (MartiniException e) {
			LOGGER.error("unable to create Spring context", e);
			Gui.reportError(this, e);
		}
		catch (Exception e) {
			LOGGER.error("unable to create Spring context", e);
			Gui.reportError(this, "error.creating.spring.context", e);
		}
	}

	protected SuiteIdentifier setUpSuiteIdentifier(ConfigurableApplicationContext context) {
		SuiteIdentifier suiteIdentifier = JMeterSuiteIdentifier.builder()
			.setJMeterContext(getThreadContext())
			.setConfigurableApplicationContext(context)
			.build();
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		beanFactory.registerSingleton(BEAN_SUITE_IDENTIFIER, suiteIdentifier);
		return suiteIdentifier;
	}

	protected PropertySource getEnvironmentPropertySource() {
		Arguments environmentArguments = getEnvironment();
		Map<String, String> variables = environmentArguments.getArgumentsAsMap();

		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		builder.putAll(variables);

		String featureLocations = this.getFeatureLocations().trim();
		if (!featureLocations.isEmpty()) {
			builder.put("martini.feature.resources", featureLocations);
		}
		ImmutableMap<String, Object> source = builder.build();

		return new MapPropertySource(PROPERTY_SYSTEM_PROPERTIES, source);
	}

	protected ConfigurableApplicationContext setUpSpring(PropertySource propertySource) {
		String[] locations = getRuntimeConfigurationLocations();
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(locations, false);

		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addFirst(propertySource);

		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	private String[] getRuntimeConfigurationLocations() {
		String joined = getConfigLocations();
		List<String> configured = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(joined);
		return configured.toArray(new String[configured.size()]);
	}

	protected void setSpringProperty(ApplicationContext context) {
		JMeterProperty property = new ObjectProperty(KEY_SPRING_CONTEXT, context);
		super.setProperty(property);
		super.setTemporary(property);
	}

	protected void setSpringVariable(ApplicationContext context) {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		variables.putObject(KEY_SPRING_CONTEXT, context);
	}

	public void testEnded() {
		LOGGER.debug("in testEnded()");

		ConfigurableApplicationContext context = getSpringContext().orElse(null);
		this.removeProperty(KEY_SPRING_CONTEXT);
		if (null != context) {
			try {
				EventManager eventManager = context.getBean(EventManager.class);
				SuiteIdentifier suiteIdentifier = context.getBean(SuiteIdentifier.class);
				eventManager.publishAfterSuite(this, suiteIdentifier);
				context.close();
			}
			catch (Exception e) {
				LOGGER.warn("unable to close Spring context", e);
				Gui.reportError(this, "error.closing.spring.context", e);
			}
		}
	}

	public void testEnded(String host) {
		LOGGER.debug("in testEnded({})", host);
		testEnded();
	}

	public void setConfigLocations(String s) {
		setStringProperty(s, PROPERTY_CONFIG_LOCATIONS, DEFAULT_RESOURCES_CONTEXT);
	}

	public String getConfigLocations() {
		return getPropertyAsString(PROPERTY_CONFIG_LOCATIONS);
	}

	public void setFeatureLocations(String s) {
		setStringProperty(s, PROPERTY_FEATURE_LOCATIONS, DEFAULT_RESOURCES_FEATURES);
	}

	public String getFeatureLocations() {
		return getPropertyAsString(PROPERTY_FEATURE_LOCATIONS);
	}

	protected void setStringProperty(String s, String propertyName, String defaultValue) {
		String trimmed = null == s ? "" : s.trim();
		setProperty(propertyName, trimmed.isEmpty() ? defaultValue : trimmed);
	}

	public void setEnvironment(Arguments environment) {
		TestElementProperty property = new TestElementProperty(PROPERTY_SYSTEM_PROPERTIES, environment);
		setProperty(property);
	}

	public Arguments getEnvironment() {
		JMeterProperty property = getProperty(PROPERTY_SYSTEM_PROPERTIES);
		Arguments arguments = null;
		if (TestElementProperty.class.isInstance(property)) {
			TestElementProperty testElementProperty = TestElementProperty.class.cast(property);
			TestElement element = testElementProperty.getElement();
			if (Arguments.class.isInstance(element)) {
				arguments = Arguments.class.cast(element);
			}
		}
		return arguments;
	}
}
