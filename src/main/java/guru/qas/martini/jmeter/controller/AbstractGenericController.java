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

import javax.annotation.Nonnull;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;

import com.google.common.base.Throwables;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.ResourceBundleMessageFunction;

import static com.google.common.base.Preconditions.checkNotNull;
import static guru.qas.martini.jmeter.controller.AbstractGenericControllerMessages.*;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractGenericController extends GenericController
	implements Serializable, Cloneable, TestBean, TestStateListener {

	private static final long serialVersionUID = -1015565560550196871L;

	// Shared.
	protected transient BeanInfoSupport beanInfoSupport;
	protected transient IMessageConveyor messageConveyor;
	protected transient Function<String, String> messageFunction;
	protected transient LocLogger logger;

	public AbstractGenericController() {
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

	protected void setUp() {
		try {
			setUpBeanInfoSupport();
			setUpLogger();
			setUpMessageFunction();
			logger.info(STARTING, getName());
			completeSetup();
		}
		catch (Exception e) {
			JMeterContextService.endTest();
			reportException(e);
			tearDown();
			throw new ThreadDeath();
		}
	}

	protected void reportException(Exception e) {
		try {
			String message = messageConveyor.getMessage(ERROR_IN_START_UP, getName());
			logger.error(message, e);

			if (null != GuiPackage.getInstance()) {
				String stacktrace = Throwables.getStackTraceAsString(e);
				String title = messageConveyor.getMessage(GUI_ERROR_TITLE, getName());
				JMeterUtils.reportErrorToUser(stacktrace, title, e);
			}
		}
		catch (Exception ignored) {
			e.printStackTrace();
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
		Locale locale = JMeterUtils.getLocale();
		messageConveyor = new MessageConveyor(locale);
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	protected abstract void completeSetup() throws Exception;

	@Override
	public Object clone() {
		Object o = super.clone();
		AbstractGenericController clone = AbstractGenericController.class.cast(o);
		clone.beanInfoSupport = beanInfoSupport;
		clone.messageConveyor = messageConveyor;
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
			String message = messageConveyor.getMessage(AbstractGenericControllerMessages.ERROR_IN_TEAR_DOWN, getName());
			logger.warn(message, e);
		}
		logger = null;
		messageConveyor = null;
		beanInfoSupport = null;
	}

	protected abstract void beginTearDown() throws Exception;
}
