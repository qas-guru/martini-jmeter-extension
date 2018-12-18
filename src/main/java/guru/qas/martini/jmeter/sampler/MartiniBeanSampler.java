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

package guru.qas.martini.jmeter.sampler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;

import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.jmeter.DefaultExceptionReporter;
import guru.qas.martini.jmeter.ExceptionReporter;

import static guru.qas.martini.jmeter.sampler.MartiniBeanSamplerMessages.*;

@SuppressWarnings("WeakerAccess")
public class MartiniBeanSampler extends AbstractSampler
	implements Serializable, Cloneable, TestStateListener, TestBean {

	private static final long serialVersionUID = -8693642618909458802L;

	// These must match field names exactly.
	protected static final String PROPERTY_BEAN_IMPLEMENTATION = "beanImplementation";
	protected static final String PROPERTY_BEAN_NAME = "beanName";
	protected static final String PROPERTY_BEAN_PROPERTIES = "beanProperties";

	// Serialized.
	protected String beanImplementation;
	protected String beanName;
	protected List<Argument> beanProperties;

	// Per-thread.
	protected transient boolean started;
	protected transient String host;
	protected transient MessageConveyor messageConveyor;
	protected transient LocLogger logger;
	protected transient ExceptionReporter reporter;

	public String getBeanImplementation() {
		return beanImplementation;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanImplementation(String beanImplementation) {
		this.beanImplementation = beanImplementation;
	}

	public String getBeanName() {
		return beanName;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public List<Argument> getBeanProperties() {
		return beanProperties;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanProperties(List<Argument> beanProperties) {
		this.beanProperties = beanProperties;
	}

	public MartiniBeanSampler() {
		super();
		init();
	}

	public Object readResolve() {
		init();
		return this;
	}

	protected void init() {
		started = false;
		beanProperties = new ArrayList<>();
	}

	@Override
	public Object clone() {
		Object clone;
		if (started) { // TODO:
			/*
						try {
				BeanController delegate = getDelegate();
				setProperties(delegate);
				delegate.setRunningVersion(true);
				notifyStarted(delegate);
				clone = delegate;
			}
			catch (Exception e) {
				throw new AssertionError(e);
			}
			 */
			throw new UnsupportedOperationException();
		}
		else {
			clone = super.clone();
		}
		return clone;
	}

	@Override
	public void testStarted() {
		setUp();
	}

	@Override
	public void testStarted(String host) {
		this.host = host;
		setUp();
	}

	@SuppressWarnings("Duplicates")
	protected void setUp() {
		try {
			setUpMessageConveyor();
			setUpLogger();
			setUpExceptionReporter();
			logger.info(STARTING, getName());
		}
		catch (Exception e) {
			JMeterContextService.endTest();
			if (null == reporter) {
				reporter = new DefaultExceptionReporter();
			}
			reporter.logException(ERROR_IN_START_UP, e, getName());
			reporter.showException(GUI_ERROR_TITLE, e, getName());
			tearDown();
			throw new ThreadDeath();
		}
		// TODO: validate input
	}

	protected void setUpMessageConveyor() {
		Locale locale = JMeterUtils.getLocale();
		messageConveyor = new MessageConveyor(locale);
	}

	protected void setUpLogger() {
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	protected void setUpExceptionReporter() {
		reporter = new DefaultExceptionReporter(messageConveyor, logger);
	}

	@Override
	public SampleResult sample(Entry e) {
		throw new UnsupportedOperationException();
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
		messageConveyor = null;
		logger = null;
		host = null;
		started = false;
	}
}
