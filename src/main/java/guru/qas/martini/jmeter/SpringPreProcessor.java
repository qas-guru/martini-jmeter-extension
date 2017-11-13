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

package guru.qas.martini.jmeter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Splitter;

import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.runtime.event.EventManager;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings({"unused", "WeakerAccess"}) // Referenced by JMeter.
public final class SpringPreProcessor extends AbstractTestElement
	implements PreProcessor, TestStateListener, LoopIterationListener, NoThreadClone {

	private static final long serialVersionUID = 5513210063612854545L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringPreProcessor.class);
	public static final String VARIABLE = "spring.application.context";

	public static final String ARGUMENT_LOCATIONS = "martini.spring.config.locations";
	private static final String DEFAULT_LOCATIONS = "classpath*:/martiniJmeterContext.xml";

	private static final Arguments ARGUMENTS = new Arguments();

	static {
		ARGUMENTS.addArgument(ARGUMENT_LOCATIONS, DEFAULT_LOCATIONS, null,
			"Spring XML application configuration file locations, comma-separated");
	}

	private transient AtomicReference<ClassPathXmlApplicationContext> ref;
	private transient SuiteIdentifier suiteIdentifier;
	private transient EventManager eventManager;

	public SpringPreProcessor() {
		super();
	}

	@Override
	public void testStarted() {
		// TODO: are we creating an output file?
		ref = new AtomicReference<>();
		String[] locations = getLocations();
		testStarted(locations);
	}

	private String[] getLocations() {
		String property = super.getPropertyAsString(ARGUMENT_LOCATIONS).trim();
		List<String> locations = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(property);
		LinkedHashSet<String> locationSet = new LinkedHashSet<>(locations);
		checkState(!locationSet.isEmpty(), "no Spring configuration locations provided");
		return locationSet.toArray(new String[locationSet.size()]);
	}

	private void testStarted(String[] configLocations) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations);
		context.registerShutdownHook();
		context.refresh();

		eventManager = context.getBean(EventManager.class);
		suiteIdentifier = context.getBean(SuiteIdentifier.class);
		eventManager.publishBeforeSuite(this, suiteIdentifier);

		ref.set(context);
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		TestElement source = event.getSource();
		JMeterContext threadContext = source.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		iterationStart(variables);
	}

	private void iterationStart(JMeterVariables variables) {
		Object object = variables.getObject(VARIABLE);
		if (null == object) {
			variables.putObject(VARIABLE, ref.get());
		}
	}

	@Override
	public void process() {
	}

	public static ApplicationContext getApplicationContext() {
		JMeterContext context = JMeterContextService.getContext();
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(VARIABLE);
		checkNotNull(o, "variable %s not found", VARIABLE);
		return ApplicationContext.class.cast(o);
	}

	public static <T> T getBean(Class<T> implementation) {
		checkNotNull(implementation, "null Class");
		ApplicationContext applicationContext = getApplicationContext();
		return applicationContext.getBean(implementation);
	}

	@Override
	public void testEnded() {
		ClassPathXmlApplicationContext applicationContext = ref.get();
		if (null != eventManager && null != suiteIdentifier) {
			eventManager.publishAfterSuite(this, suiteIdentifier);
			close(applicationContext);
		}
		ref = null;
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}

	private static void close(ClassPathXmlApplicationContext context) {
		try {
			context.close();
		}
		catch (Exception e) {
			LOGGER.warn("unable to close ClassPathXmlApplicationContext", e);
		}
	}

	public static Arguments getDefaultArguments() {
		return ARGUMENTS;
	}
}
