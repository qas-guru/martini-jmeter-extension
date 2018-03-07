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
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

@SuppressWarnings("WeakerAccess")
public final class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final long serialVersionUID = 6143536078921717477L;
	protected static final String PROPERTY_CONFIG_LOCATIONS = "martini.config.locations";
	protected static final String PROPERTY_SYSTEM_PROPERTIES = "martini.system.properties";
	private static final String PROPERTY_SPRING_CONTEXT = "martini.spring.context";

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniPreProcessor.class);

	public MartiniPreProcessor() {
		super();
	}

	public void process() {
		LOGGER.debug("in process()");
		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		samplerContext.computeIfAbsent(PROPERTY_SPRING_CONTEXT, s -> {
			ClassPathXmlApplicationContext context = getSpringContext().orElse(null);
			if (null == context) {
				LOGGER.warn("Spring context not set");
			}
			return context;
		});
	}

	private Optional<ClassPathXmlApplicationContext> getSpringContext() {
		JMeterProperty property = this.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = property.getObjectValue();
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.isInstance(o) ?
			ClassPathXmlApplicationContext.class.cast(o) : null;
		return Optional.ofNullable(context);
	}

	public void testStarted() {
		LOGGER.debug("in testStarted()");

		try {
			PropertySource propertySource = getEnvironmentPropertySource();
			ConfigurableApplicationContext context = setUpSpring(propertySource);
			JMeterProperty property = new ObjectProperty(PROPERTY_SPRING_CONTEXT, context);
			super.setProperty(property);
			super.setTemporary(property);
		}
		catch (Exception e) {
			LOGGER.error("unable to create Spring context", e);
		}
	}

	public void testStarted(String host) {
		LOGGER.debug("in testStarted({})", host);
	}

	protected PropertySource getEnvironmentPropertySource() {
		Arguments environmentArguments = getEnvironment();
		Map<String, String> variables = environmentArguments.getArgumentsAsMap();
		Map<String, Object> cast = ImmutableMap.<String, Object>builder().putAll(variables).build();
		return new MapPropertySource(PROPERTY_SYSTEM_PROPERTIES, cast);
	}

	protected ConfigurableApplicationContext setUpSpring(PropertySource propertySource) {
		String joined = getConfigLocations();
		List<String> locations = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(joined);
		String[] locationArray = locations.toArray(new String[locations.size()]);
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(locationArray, false);

		ConfigurableEnvironment environment = context.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addFirst(propertySource);

		context.refresh();
		context.registerShutdownHook();
		return context;
	}

	public void testEnded() {
		LOGGER.debug("in testEnded()");
		ClassPathXmlApplicationContext context = getSpringContext().orElse(null);
		this.removeProperty(PROPERTY_SPRING_CONTEXT);
		if (null != context) {
			try {
				context.close();
			}
			catch (Exception e) {
				LOGGER.warn("unable to close Spring context", e);
			}
		}
	}

	public void testEnded(String host) {
		LOGGER.debug("in testEnded({})", host);
	}

	public void setConfigLocations(String configLocations) {
		setProperty(PROPERTY_CONFIG_LOCATIONS, configLocations);
	}

	public String getConfigLocations() {
		return getPropertyAsString(PROPERTY_CONFIG_LOCATIONS);
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
