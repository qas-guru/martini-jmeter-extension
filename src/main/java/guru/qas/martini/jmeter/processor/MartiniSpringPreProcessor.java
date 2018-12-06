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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import guru.qas.martini.MartiniException;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.JMeterContextUtil;
import guru.qas.martini.runtime.event.EventManager;

import static guru.qas.martini.jmeter.JMeterContextUtil.*;

@SuppressWarnings("WeakerAccess")
@Deprecated
public final class MartiniSpringPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 6143536078921717477L;

	public static final String DEFAULT_RESOURCES_CONTEXT = "classpath*:**/contextOne.xml,classpath*:**/contextTwo.xml";
	public static final String DEFAULT_RESOURCES_FEATURES = "classpath*:**/*.feature";

	protected static final String PROPERTY_CONFIG_LOCATIONS = "martini.spring.config.locations";
	protected static final String PROPERTY_FEATURE_LOCATIONS = "martini.feature.locations";
	protected static final String PROPERTY_SYSTEM_PROPERTIES = "martini.system.properties";
	protected static final String BEAN_SUITE_IDENTIFIER = "suiteIdentifier";

	protected transient MessageSource messageSource;
	protected transient Logger logger;

	public MartiniSpringPreProcessor() {
		super();
		init();
	}

	protected void init() {
		messageSource = MessageSources.getMessageSource(getClass());
		logger = LoggerFactory.getLogger(getClass());
	}

	protected Object readResolve() {
		init();
		return this;
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

			setTemporaryProperty(this, springContext, ConfigurableApplicationContext.class);
			setVariable(springContext, ApplicationContext.class);
		}
		catch (MartiniException e) {
			StandardJMeterEngine.stopEngineNow();
			logger.error(e.getMessage(), e);
			Gui.reportError(this, e);
		}
		catch (Exception e) {
			StandardJMeterEngine.stopEngineNow();
			MartiniException martiniException = new MartiniException.Builder()
				.setLocale(JMeterUtils.getLocale())
				.setMessageSource(messageSource)
				.setKey("error.creating.spring.context")
				.setCause(e)
				.build();
			logger.error(martiniException.getMessage(), e);
			Gui.reportError(this, martiniException);
		}
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

	@Override
	public void iterationStart(LoopIterationEvent event) {
		ApplicationContext springContext = getSpringContextProperty();
		if (null == springContext) {
			JMeterContextUtil.removeVariable(ApplicationContext.class);
		}
		else {
			setVariable(springContext, ApplicationContext.class);
		}
	}

	public void process() {
		ConfigurableApplicationContext springContext = getSpringContextProperty();
		if (null == springContext) {
			JMeterContextUtil.removeSamplerData(ApplicationContext.class);
		}
		else {
			JMeterContextUtil.setSamplerData(springContext, ApplicationContext.class);
		}
	}

	private ConfigurableApplicationContext getSpringContextProperty() {
		return JMeterContextUtil.getProperty(this, ConfigurableApplicationContext.class).orElse(null);
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
		List<String> configured = null == joined ?
			ImmutableList.of() : Splitter.on(',').omitEmptyStrings().trimResults().splitToList(joined);
		return configured.toArray(new String[0]);
	}

	public void testEnded() {
		ConfigurableApplicationContext context = getSpringContextProperty();
		JMeterContextUtil.removeProperty(this, ConfigurableApplicationContext.class);
		if (null != context) {
			try {
				EventManager eventManager = context.getBean(EventManager.class);
				SuiteIdentifier suiteIdentifier = context.getBean(SuiteIdentifier.class);
				eventManager.publishAfterSuite(this, suiteIdentifier);
				context.close();
			}
			catch (Exception e) {
				MartiniException martiniException = new MartiniException.Builder()
					.setCause(e)
					.setLocale(JMeterUtils.getLocale())
					.setMessageSource(messageSource)
					.setKey("error.closing.spring.context")
					.build();
				logger.warn(martiniException.getMessage(), e);
				Gui.reportError(this, martiniException);
			}
		}
	}

	public void testEnded(String host) {
		testEnded();
	}

	public void setConfigLocations(String s) {
		setProperty(PROPERTY_CONFIG_LOCATIONS, s);
	}

	public String getConfigLocations() {
		return getPropertyAsString(PROPERTY_CONFIG_LOCATIONS);
	}

	public void setFeatureLocations(String s) {
		setProperty(PROPERTY_FEATURE_LOCATIONS, s);
	}

	public String getFeatureLocations() {
		return getPropertyAsString(PROPERTY_FEATURE_LOCATIONS);
	}

	public void setEnvironment(Arguments environment) {
		TestElementProperty property = new TestElementProperty(PROPERTY_SYSTEM_PROPERTIES, environment);
		setProperty(property);
	}

	public static ApplicationContext getApplicationContext() {
		return JMeterContextUtil.getVariable(ApplicationContext.class).orElseThrow(() ->
			new MartiniException.Builder()
				.setLocale(JMeterUtils.getLocale())
				.setMessageSource(MessageSources.getMessageSource(MartiniSpringPreProcessor.class))
				.setKey("spring.not.loaded")
				.build());
	}
}
