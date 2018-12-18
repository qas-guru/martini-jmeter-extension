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

package guru.qas.martini.jmeter.controller;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Function;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.ResourceBundleMessageFunction;
import guru.qas.martini.jmeter.DefaultExceptionReporter;
import guru.qas.martini.jmeter.ExceptionReporter;

import static guru.qas.martini.jmeter.controller.AbstractGenericControllerMessages.*;
import static guru.qas.martini.jmeter.sampler.MartiniBeanSamplerMessages.ERROR_IN_START_UP;
import static guru.qas.martini.jmeter.sampler.MartiniBeanSamplerMessages.GUI_ERROR_TITLE;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractGenericController extends GenericController
	implements Serializable, Cloneable, TestBean, TestStateListener {

	private static final long serialVersionUID = -1015565560550196871L;

	// Shared.
	protected transient BeanInfoSupport beanInfoSupport;
	protected transient IMessageConveyor messageConveyor;
	protected transient Function<String, String> messageFunction;
	protected transient LocLogger logger;
	protected String host;
	protected transient ExceptionReporter reporter;

	public AbstractGenericController() {
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
			setUpMessageConveyor();
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
		AbstractGenericController clone = AbstractGenericController.class.cast(o);
		clone.beanInfoSupport = beanInfoSupport;
		clone.messageConveyor = messageConveyor;
		clone.logger = logger;
		clone.host = host;
		clone.reporter = reporter;
		return clone;
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
		messageConveyor = null;
		beanInfoSupport = null;
		reporter = null;
		host = null;
	}

	protected abstract void beginTearDown() throws Exception;
}
