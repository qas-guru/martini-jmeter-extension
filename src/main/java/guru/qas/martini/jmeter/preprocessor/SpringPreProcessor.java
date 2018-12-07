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

package guru.qas.martini.jmeter.preprocessor;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;

import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.jmeter.ArgumentListPropertySource;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.preprocessor.SpringPreProcessorMessages.*;
import static org.springframework.core.env.StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;

/**
 * Manages a Spring ClassPathXmlApplicationContext, making the context accessible to setup and test threads
 * through JMeterVariables as SpringPreProcessor.THREAD_CONTEXT_VARIABLE "martini.spring.application.context".
 * It also makes the ClassPathXmlApplicationContext available to Samplers via the SamplerContext map as
 * SpringPreProcessor.SAMPLER_CONTEXT_KEY "martini.spring.application.context".
 * "
 * <p>
 * One enabled SpringPreProcessor should be configured at the top-level of the test plan before
 * any ThreadGroup configurations.
 */
@SuppressWarnings("WeakerAccess")
public class SpringPreProcessor
	extends AbstractTestElement
	implements Serializable, Cloneable, PreProcessor, TestBean, TestStateListener {

	private static final long serialVersionUID = -1582951167073002597L;
	protected static final AtomicReference<ClassPathXmlApplicationContext> CONTEXT_REF = new AtomicReference<>(null);

	// These must match field names exactly.
	protected static final String PROPERTY_SPRING_CONFIG_LOCATIONS = "configurationLocations";
	protected static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";

	public static final String THREAD_CONTEXT_VARIABLE = "martini.spring.application.context";
	public static final String SAMPLER_CONTEXT_KEY = THREAD_CONTEXT_VARIABLE;

	// Serialized.
	protected List<Argument> environmentVariables;
	protected List<String> configurationLocations;

	// Shared.
	protected transient SpringPreProcessorBeanInfo beanInfo;
	protected transient IMessageConveyor messageConveyor;
	protected transient LocLogger logger;
	protected transient boolean usingIdMessages;

	public List<Argument> getEnvironmentVariables() {
		return environmentVariables;
	}

	@SuppressWarnings("unused") // Accessed via bean introspection.
	public void setEnvironmentVariables(List<Argument> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public List<String> getConfigurationLocations() {
		return configurationLocations;
	}

	@SuppressWarnings("unused") // Accessed via bean introspection.
	public void setConfigurationLocations(List<String> configurationLocations) {
		this.configurationLocations = configurationLocations;
	}

	@Override
	public void testStarted() {
		setUp();
	}

	@Override
	public void testStarted(String host) {
		setUp();
	}

	protected void setUp() {
		try {
			setUpBeanInfo();
			setUpLogger();
			setUpMessaging();

			logger.info(STARTING, super.getName());
			setUpSpringContext();
		}
		catch (Exception e) { // Last-ditch effort to warn user of unexpected exception.
			Throwable t = e.fillInStackTrace();
			t.printStackTrace();
		}
	}

	protected void setUpBeanInfo() {
		beanInfo = new SpringPreProcessorBeanInfo();
	}

	protected void setUpLogger() {
		Locale locale = JMeterUtils.getLocale();
		messageConveyor = new MessageConveyor(locale);
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	protected void setUpMessaging() {
		String displayName = beanInfo.getDisplayName();
		String trimmed = null == displayName ? "" : displayName.trim();
		usingIdMessages = super.getName().trim().equals(trimmed);
	}

	protected void setUpSpringContext() {
		try {
			String[] locations = getLocations();
			setUpSpringContext(locations);
		}
		catch (Exception e) {
			JMeterContextService.endTest();
			String message = messageConveyor.getMessage(SPRING_STARTUP_ERROR, getName());
			Throwable throwable = e.fillInStackTrace();
			logger.error(message, throwable);
			if (null != GuiPackage.getInstance()) {
				String errorMessage = String.format("%s;\n%s.", messageConveyor.getMessage(ERROR_MESSAGE), e.getMessage());
				String title = messageConveyor.getMessage(ERROR_TITLE, getName());
				JMeterUtils.reportErrorToUser(errorMessage, title, e);
			}
			tearDown();
			throw new ThreadDeath();
		}
	}

	protected String[] getLocations() {
		List<String> configured = getConfigurationLocations();
		checkNotNull(configured,
			messageConveyor.getMessage(MISSING_PROPERTY, beanInfo.getDisplayName(PROPERTY_SPRING_CONFIG_LOCATIONS)));

		List<String> locations = configured.stream()
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(item -> !item.isEmpty())
			.collect(Collectors.toList());
		checkArgument(!locations.isEmpty(),
			messageConveyor.getMessage(EMPTY_PROPERTY, beanInfo.getDisplayName(PROPERTY_SPRING_CONFIG_LOCATIONS)));
		return locations.toArray(new String[]{});
	}

	protected void setUpSpringContext(String[] locations) {
		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(locations, false);
		checkState(CONTEXT_REF.compareAndSet(null, springContext), messageConveyor.getMessage(DUPLICATE_SPRING_CONTEXT));
		setEnvironment(springContext);
		springContext.refresh();
		springContext.registerShutdownHook();
		setUpSpringContextVariable();
	}

	protected void setEnvironment(ClassPathXmlApplicationContext context) {
		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		ArgumentListPropertySource propertySource = getJMeterPropertySource();
		propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
	}

	protected ArgumentListPropertySource getJMeterPropertySource() {
		String name = super.getName();
		List<Argument> environmentVariables = getEnvironmentVariables();
		checkNotNull(environmentVariables,
			messageConveyor.getMessage(MISSING_PROPERTY, beanInfo.getDisplayName(PROPERTY_ENVIRONMENT_VARIABLES)));
		return ArgumentListPropertySource.builder().setName(name).setArguments(environmentVariables).build();
	}

	protected void setUpSpringContextVariable() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		ClassPathXmlApplicationContext applicationContext = CONTEXT_REF.get();
		variables.putObject(THREAD_CONTEXT_VARIABLE, applicationContext);
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		SpringPreProcessor clone = SpringPreProcessor.class.cast(o);
		clone.beanInfo = beanInfo;
		clone.messageConveyor = messageConveyor;
		clone.logger = logger;
		return clone;
	}

	@Override
	public void process() {
		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		samplerContext.put(SAMPLER_CONTEXT_KEY, CONTEXT_REF.get());
	}

	@Override
	public void testEnded() {
		tearDown();
	}

	@Override
	public void testEnded(String host) {
		tearDown();
	}

	protected void tearDown() {
		tearDownSpring();
		logger = null;
		messageConveyor = null;
		beanInfo = null;
	}

	private void tearDownSpring() {
		ClassPathXmlApplicationContext springContext = CONTEXT_REF.getAndSet(null);
		if (null != springContext) {
			try {
				springContext.close();
			}
			catch (Exception e) {
				logger.warn(SPRING_CLOSE_EXCEPTION, getName(), e);
			}
		}
	}
}
