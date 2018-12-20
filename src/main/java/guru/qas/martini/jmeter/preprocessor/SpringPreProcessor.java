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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestIterationListener;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

import guru.qas.martini.jmeter.ArgumentListPropertySource;
import guru.qas.martini.jmeter.Messages;
import guru.qas.martini.jmeter.SamplerContext;
import guru.qas.martini.jmeter.Variables;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.Messages.getMessage;
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
	extends AbstractPreProcessor
	implements Serializable, Cloneable, TestBean, TestIterationListener, Thread.UncaughtExceptionHandler {

	private static final long serialVersionUID = -1582951167073002597L;
	protected static final AtomicReference<ClassPathXmlApplicationContext> CONTEXT_REF = new AtomicReference<>(null);

	// These must match field names exactly.
	protected static final String PROPERTY_SPRING_CONFIG_LOCATIONS = "configurationLocations";
	protected static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";

	// Serialized.
	protected List<Argument> environmentVariables;
	protected List<String> configurationLocations;

	// Per-thread.
	protected ThreadLocal<Thread.UncaughtExceptionHandler> setUpExceptionHandler;

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
	protected BeanInfoSupport getBeanInfoSupport() {
		return new SpringPreProcessorBeanInfo();
	}

	@Override
	protected void completeSetup() {
		setUpExceptionHandler = new ThreadLocal<>();
		setUpExceptionHandler.set(Thread.getDefaultUncaughtExceptionHandler());
		Thread.setDefaultUncaughtExceptionHandler(this);
		setUpSpringContext();
	}

	protected void setUpSpringContext() {
		String[] locations = getLocations();
		setUpSpringContext(locations);
	}

	protected String[] getLocations() {
		List<String> configured = getConfigurationLocations();
		checkNotNull(configured,
			getMessage(MISSING_PROPERTY, getDisplayName(PROPERTY_SPRING_CONFIG_LOCATIONS)));

		List<String> locations = configured.stream()
			.filter(Objects::nonNull)
			.map(String::trim)
			.filter(item -> !item.isEmpty())
			.collect(Collectors.toList());
		checkArgument(!locations.isEmpty(),
			getMessage(EMPTY_PROPERTY, getDisplayName(PROPERTY_SPRING_CONFIG_LOCATIONS)));
		return locations.toArray(new String[]{});
	}

	protected void setUpSpringContext(String[] locations) {
		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(locations, false);
		checkState(CONTEXT_REF.compareAndSet(null, springContext), getMessage(DUPLICATE_SPRING_CONTEXT));
		springContext.setDisplayName(this.getName());
		setEnvironment(springContext);
		springContext.refresh();
		springContext.registerShutdownHook();
		Variables.set(springContext);
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
		checkNotNull(environmentVariables, getMessage(MISSING_PROPERTY, getDisplayName(PROPERTY_ENVIRONMENT_VARIABLES)));
		return ArgumentListPropertySource.builder().setName(name).setArguments(environmentVariables).build();
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		ClassPathXmlApplicationContext springContext = CONTEXT_REF.get();
		Variables.set(springContext);
	}

	@Override
	public void process() {
		ClassPathXmlApplicationContext springContext = CONTEXT_REF.get();
		SamplerContext.set(springContext);
	}

	@Override
	protected void beginTearDown() {
		tearDownSpring();
	}

	private void tearDownSpring() {
		ClassPathXmlApplicationContext springContext = CONTEXT_REF.getAndSet(null);
		if (null != springContext) {
			springContext.close();
		}
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		super.tearDown();
		if (null != setUpExceptionHandler) {
			Thread.UncaughtExceptionHandler delegate = setUpExceptionHandler.get();
			setUpExceptionHandler.remove();
			if (null != delegate) {
				delegate.uncaughtException(t, e);
			}
		}
	}
}
