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
import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Splitter;

@SuppressWarnings("WeakerAccess")
public final class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final long serialVersionUID = 6143536078921717477L;
	protected static final String PROPERTY_CONFIG_LOCATIONS = "martini.config.locations";
	protected static final String PROPERTY_ENVIRONMENT = "martini.system.environment";
	private static final String PROPERTY_SPRING_CONTEXT = "martini.spring.context";

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniPreProcessor.class);

	public MartiniPreProcessor() {
		super();
	}

	public void process() {
		LOGGER.debug("in process()");
		ClassPathXmlApplicationContext context = getClassPathXmlApplicationContext().orElse(null);
		if (null == context) {
			System.out.println("breakpoint");
		}
		else {
			System.out.println("breakpoint");
		}
	}

	private Optional<ClassPathXmlApplicationContext> getClassPathXmlApplicationContext() {
		JMeterProperty property = this.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = property.getObjectValue();
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.isInstance(o) ?
			ClassPathXmlApplicationContext.class.cast(o) : null;
		return Optional.ofNullable(context);
	}

	public void testStarted() {
		LOGGER.debug("in testStarted()");

		try {
			setUpEnvironment();
			String joined = getConfigLocations();
			List<String> locations = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(joined);
			String[] locationArray = locations.toArray(new String[locations.size()]);
			ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(locationArray);
			ObjectProperty property = new ObjectProperty(PROPERTY_SPRING_CONTEXT, context);
			this.setTemporary(property);
		}
		catch (Exception e) {
			LOGGER.error("unable to create Spring context", e);
		}
	}

	public void testStarted(String host) {
		LOGGER.debug("in testStarted({})", host);
	}

	protected void setUpEnvironment() {
		Arguments environment = getEnvironment();
		Map<String, String> variables = environment.getArgumentsAsMap();
		if (!variables.isEmpty()) {
			Properties systemProperties = System.getProperties();
			variables.forEach((key, value) -> {
				if (!systemProperties.containsKey(key)) {
					systemProperties.setProperty(key, value);
				}
			});
		}
	}

	public void testEnded() {
		LOGGER.debug("in testEnded()");
		JMeterProperty property = this.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = property.getObjectValue();
		if (ClassPathXmlApplicationContext.class.isInstance(o)) {
			ClassPathXmlApplicationContext applicationContext = ClassPathXmlApplicationContext.class.cast(o);
			applicationContext.close();
		}
		this.removeProperty(PROPERTY_SPRING_CONTEXT);
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
		TestElementProperty property = new TestElementProperty(PROPERTY_ENVIRONMENT, environment);
		setProperty(property);
	}

	public Arguments getEnvironment() {
		JMeterProperty property = getProperty(PROPERTY_ENVIRONMENT);
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
