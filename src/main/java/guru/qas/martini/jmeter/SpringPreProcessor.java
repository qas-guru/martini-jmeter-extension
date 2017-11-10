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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings({"unused", "WeakerAccess"}) // Referenced by JMeter.
public final class SpringPreProcessor extends AbstractTestElement
	implements PreProcessor, TestStateListener, LoopIterationListener, NoThreadClone {

	private static final long serialVersionUID = 5513210063612854545L;
	private static final Logger LOGGER = LoggerFactory.getLogger(SpringPreProcessor.class);
	public static final String KEY_CONFIG_LOCATIONS = "config.locations";
	public static final String DEFAULT_CONFIG_LOCATIONS = "classpath*:martiniJmeterContext.xml";
	public static final String KEY_APPLICATION_CONTEXT = "spring.application.context";

	private transient AtomicReference<ClassPathXmlApplicationContext> ref;

	public SpringPreProcessor() {
		super();
		super.setProperty(KEY_CONFIG_LOCATIONS, DEFAULT_CONFIG_LOCATIONS);
	}

	@Override
	public void testStarted() {
		ref = new AtomicReference<>();
		String[] configLocations = getConfigLocations();
		testStarted(configLocations);
	}

	private String[] getConfigLocations() {
		String property = super.getPropertyAsString(KEY_CONFIG_LOCATIONS, "").trim();
		checkState(!property.isEmpty(), "no configLocations set");
		return getConfigLocations(property);
	}

	private static String[] getConfigLocations(String property) {
		List<String> split = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(property);
		checkState(!split.isEmpty(), "no configLocations set");
		return getConfigLocations(split);
	}

	private static String[] getConfigLocations(List<String> configLocations) {
		Set<String> unique = new LinkedHashSet<>(configLocations);
		return unique.toArray(new String[unique.size()]);
	}

	private void testStarted(String[] configLocations) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocations);
		context.registerShutdownHook();
		testStarted(context);
	}

	private void testStarted(ClassPathXmlApplicationContext context) {
		context.refresh();
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
		Object object = variables.getObject(KEY_APPLICATION_CONTEXT);
		if (null == object) {
			variables.putObject(KEY_APPLICATION_CONTEXT, ref.get());
		}
	}

	@Override
	public void process() {
	}

	@Override
	public void testEnded() {
		ClassPathXmlApplicationContext applicationContext = ref.get();
		ref = null;
		if (null != applicationContext) {
			close(applicationContext);
		}
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
}
