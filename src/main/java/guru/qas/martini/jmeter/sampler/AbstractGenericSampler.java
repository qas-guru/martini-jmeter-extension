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
import java.util.function.Function;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import com.google.common.base.Throwables;

import ch.qos.cal10n.IMessageConveyor;
import guru.qas.martini.Messages;
import guru.qas.martini.ResourceBundleMessageFunction;
import guru.qas.martini.jmeter.DefaultExceptionReporter;
import guru.qas.martini.jmeter.ExceptionReporter;

import static guru.qas.martini.jmeter.sampler.AbstractGenericSamplerMessages.*;

@SuppressWarnings({"WeakerAccess", "Duplicates"})
public abstract class AbstractGenericSampler extends AbstractSampler
	implements Serializable, Cloneable, TestBean, TestStateListener {

	private static final long serialVersionUID = -4371566971973617334L;

	// Shared.
	protected transient BeanInfoSupport beanInfoSupport;
	protected transient Function<String, String> messageFunction;
	protected transient LocLogger logger;
	protected transient String host;
	protected transient ExceptionReporter reporter;

	public AbstractGenericSampler() {
		super();
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
			setUpLogger();
			setUpExceptionReporter();
			logger.info(STARTING, getName());
			setUpBeanInfoSupport();
			setUpMessageFunction();
			completeSetup();
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
	}

	protected void setUpLogger() {
		IMessageConveyor messageConveyor = Messages.getMessageConveyor();
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	protected void setUpExceptionReporter() {
		reporter = new DefaultExceptionReporter(logger);
	}

	protected void setUpBeanInfoSupport() throws Exception {
		beanInfoSupport = getBeanInfoSupport();
	}

	protected abstract BeanInfoSupport getBeanInfoSupport() throws Exception;

	protected void setUpMessageFunction() {
		messageFunction = ResourceBundleMessageFunction.getInstance(beanInfoSupport);
	}

	protected abstract void completeSetup() throws Exception;

	@Override
	public Object clone() {
		Object o = super.clone();
		AbstractGenericSampler clone = AbstractGenericSampler.class.cast(o);
		clone.beanInfoSupport = beanInfoSupport;
		clone.logger = logger;
		clone.host = host;
		clone.reporter = reporter;
		return clone;
	}

	@Override
	public SampleResult sample(Entry entry) { // Entry is always null.
		SampleResult result = new SampleResult();
		result.setSampleLabel(super.getName());
		try {
			completeSample(result);
		}
		catch (Exception e) {
			result.setSuccessful(false);
			String stacktrace = Throwables.getStackTraceAsString(e);
			result.setResponseMessage(stacktrace);
		}
		return result;
	}

	protected abstract void completeSample(SampleResult result) throws Exception;

	@Override
	public void testEnded() {
		tearDown();
	}

	@Override
	public void testEnded(String host) {
		tearDown();
	}

	protected void tearDown() {
		try {
			beginTearDown();
		}
		catch (Exception e) {
			reporter.logException(ERROR_IN_TEAR_DOWN, e, getName());
		}

		logger = null;
		beanInfoSupport = null;
		reporter = null;
		host = null;
	}

	protected abstract void beginTearDown() throws Exception;
}
