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

package guru.qas.martini.jmeter.visualizers;

import org.apache.jmeter.reporters.AbstractListenerElement;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleListener;
import org.apache.jmeter.samplers.SampleMonitor;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.visualizers.Visualizer;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.springframework.context.ConfigurableApplicationContext;


import static guru.qas.martini.MartiniConstants.PROPERTY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public final class JsonWriterElement extends AbstractListenerElement implements TestBean, Visualizer, SampleListener, SampleMonitor, TestStateListener {

	private static final long serialVersionUID = 2487993535127253056L;

	private static final String PROPERTY_FILENAME = "filename";

	public String getFilename() {
		return super.getPropertyAsString(PROPERTY_FILENAME);
	}

	public void setFilename(String filename) {
		String trimmed = null == filename ? null : filename.trim();
		super.setProperty(PROPERTY_FILENAME, trimmed);
	}

	@Override
	public void setListener(Visualizer vis) {
		super.setListener(vis);
	}

	@Override
	public void clear() {
		super.clear();
	}

	protected ConfigurableApplicationContext getApplicationContext() {
		JMeterContext threadContext = super.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		JMeterProperty property = threadGroup.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = null == property ? null : property.getObjectValue();
		if (!ConfigurableApplicationContext.class.isInstance(o)) {
			throw new JMeterStopThreadException("unable to retrieve Spring ApplicationContext from ThreadGroup");
		}
		return ConfigurableApplicationContext.class.cast(o);
	}

	@Override
	public void sampleOccurred(SampleEvent e) {
		System.out.println("breakpoint");
	}


	@Override
	public void sampleStarted(SampleEvent e) {
		System.out.println("breakpoint");
	}

	@Override
	public void sampleStopped(SampleEvent e) {
		System.out.println("breakpoint");
	}

	@Override
	public void testStarted() {
		System.out.println("breakpoint");
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void testEnded() {
		// TODO: close stuff down
		System.out.println("breakpoint");
	}

	@Override
	public void testEnded(String host) {
		// TODO: close stuff down
		System.out.println("breakpoint");
	}

	@Override
	public void sampleStarting(Sampler sampler) {
		System.out.println("breakpoint");

	}

	@Override
	public void sampleEnded(Sampler sampler) {
		System.out.println("breakpoint");
	}

	@Override
	public void add(SampleResult sample) {
		System.out.println("breakpoint");
	}

	@Override
	public boolean isStats() {
		return false;
	}
}
