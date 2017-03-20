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
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import qas.guru.martini.event.SuiteEvent;

import static qas.guru.martini.MartiniConstants.VARIABLE_APPLICATION_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfiguration extends ConfigTestElement implements TestStateListener, Serializable {

	private static final long serialVersionUID = 4860248616231023021L;

	protected static final String PROPERTY_CONFIG_LOCATION = "contextLocation";
	protected static final String PROPERTY_ARGUMENTS = "arguments";

	public MartiniSpringConfiguration() {
		setArguments(new Arguments());
	}

	public void setArguments(Arguments arguments) {
		JMeterProperty property = new ObjectProperty(PROPERTY_ARGUMENTS, arguments);
		setProperty(property);
	}

	public void setConfigLocation(String location) {
		this.setProperty(PROPERTY_CONFIG_LOCATION, location);
	}

	public String getConfigLocation() {
		return getPropertyAsString(PROPERTY_CONFIG_LOCATION);
	}

	public void addArgument(String name, String value) {
		Arguments arguments = getArguments();
		arguments.addArgument(name, value);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.cast(o);
	}

	public void removeArguments() {
		setArguments(new Arguments());
	}

	@Override
	public boolean expectsModification() {
		return false;
	}

	@Override
	public void testStarted() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}

		String location = this.getConfigLocation();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{location}, false);
		// TODO: set spring profiles
		context.refresh();
		variables.putObject(VARIABLE_APPLICATION_CONTEXT, context);

		SuiteEvent event = SuiteEvent.getStarting(threadContext);
		context.publishEvent(event);
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testEnded() {
		JMeterContext threadContext = getThreadContext();

		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(VARIABLE_APPLICATION_CONTEXT);
		if (ConfigurableApplicationContext.class.isInstance(o)) {
			ConfigurableApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);

			SuiteEvent event = SuiteEvent.getEnded(threadContext);
			context.publishEvent(event);
			context.close();
		}
	}

	@Override
	public void testEnded(String host) {
	}
}
