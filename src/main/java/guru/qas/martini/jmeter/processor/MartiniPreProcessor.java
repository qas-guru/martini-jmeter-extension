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

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Splitter;

public final class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final long serialVersionUID = 6143536078921717477L;
	private static final String PROPERTY_CONFIG_LOCATIONS = "configLocations";
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniPreProcessor.class);

	public MartiniPreProcessor() {
		super();
	}

	public void process() {
		LOGGER.debug("in process()");
	}

	public void testStarted() {
		LOGGER.debug("in testStarted()");

		String joined = getConfigLocations();
		List<String> locations = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(joined);
		String[] locationArray = locations.toArray(new String[locations.size()]);
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(locationArray);
		AbstractThreadGroup threadGroup = getThreadGroup();
		ObjectProperty property = new ObjectProperty(PROPERTY_SPRING_CONTEXT, context);
		threadGroup.setTemporary(property);
	}

	private AbstractThreadGroup getThreadGroup() {
		JMeterContext threadContext = getThreadContext();
		return threadContext.getThreadGroup();
	}

	public void testStarted(String host) {
		LOGGER.debug("in testStarted({})", host);
	}

	private static final String PROPERTY_SPRING_CONTEXT = "martini.spring.context";

	public void testEnded() {
		LOGGER.debug("in testEnded()");
		AbstractThreadGroup threadGroup = getThreadGroup();
		JMeterProperty property = threadGroup.getProperty(PROPERTY_SPRING_CONTEXT);
		if (null != property) {
			threadGroup.removeProperty(PROPERTY_SPRING_CONTEXT);
			Object o = property.getObjectValue();
			if (ClassPathXmlApplicationContext.class.isInstance(o)) {
				ClassPathXmlApplicationContext applicationContext = ClassPathXmlApplicationContext.class.cast(o);
				applicationContext.close();
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
}
