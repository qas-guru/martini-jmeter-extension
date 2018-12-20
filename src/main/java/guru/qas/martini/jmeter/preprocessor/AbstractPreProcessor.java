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
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import ch.qos.cal10n.IMessageConveyor;
import guru.qas.martini.ResourceBundleMessageFunction;
import guru.qas.martini.jmeter.DefaultExceptionReporter;
import guru.qas.martini.jmeter.ExceptionReporter;
import guru.qas.martini.jmeter.Messages;

import static com.google.common.base.Preconditions.checkNotNull;
import static guru.qas.martini.jmeter.controller.AbstractGenericControllerMessages.ERROR_IN_START_UP;
import static guru.qas.martini.jmeter.controller.AbstractGenericControllerMessages.ERROR_IN_TEAR_DOWN;
import static guru.qas.martini.jmeter.controller.AbstractGenericControllerMessages.GUI_ERROR_TITLE;
import static guru.qas.martini.jmeter.preprocessor.AbstractPreProcessorMessages.*;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractPreProcessor extends AbstractTestElement
	implements Serializable, Cloneable, PreProcessor, TestBean, TestStateListener {

	private static final long serialVersionUID = -8030456516905893471L;

	// Shared.
	protected transient BeanInfoSupport beanInfoSupport;
	protected transient Function<String, String> messageFunction;
	protected transient LocLogger logger;
	protected transient ExceptionReporter reporter;

	public AbstractPreProcessor() {
		super();
	}

	@Override
	public void testStarted() {
		setUp();
	}

	@Override
	public void testStarted(String host) {
		setUp();
	}

	@SuppressWarnings("Duplicates")
	protected void setUp() {
		try {
			setUpBeanInfoSupport();
			setUpLogger();
			setUpExceptionReporter();
			setUpMessageFunction();
			logger.info(STARTING, getName());
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

	protected void setUpBeanInfoSupport() {
		beanInfoSupport = getBeanInfoSupport();
	}

	protected abstract BeanInfoSupport getBeanInfoSupport();

	protected void setUpMessageFunction() {
		messageFunction = ResourceBundleMessageFunction.getInstance(beanInfoSupport);
	}

	protected void setUpLogger() {
		IMessageConveyor messageConveyor = Messages.getMessageConveyor();
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	protected void setUpExceptionReporter() {
		reporter = new DefaultExceptionReporter(logger);
	}

	protected abstract void completeSetup() throws Exception;

	@Override
	public Object clone() {
		Object o = super.clone();
		AbstractPreProcessor clone = AbstractPreProcessor.class.cast(o);
		clone.beanInfoSupport = beanInfoSupport;
		clone.logger = logger;
		return clone;
	}

	protected String getDisplayName(@Nonnull String property) {
		checkNotNull(property, "null String");
		String key = String.format("%s.displayName", property);
		return messageFunction.apply(key);
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
		try {
			beginTearDown();
		}
		catch (Exception e) {
			reporter.logException(ERROR_IN_TEAR_DOWN, e, getName());
		}
		logger = null;
		beanInfoSupport = null;
		reporter = null;
	}

	protected abstract void beginTearDown() throws Exception;
}
