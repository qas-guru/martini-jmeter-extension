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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.util.concurrent.Monitor;

import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.runtime.event.EventManager;

import static guru.qas.martini.jmeter.preprocessor.DefaultMartiniSuitePreProcesorBeanMessages.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultMartiniSuitePreProcessorBean implements MartiniSuitePreProcessorBean {

	protected final LocLogger logger;
	protected final EventManager eventManager;
	protected final SuiteIdentifier suiteIdentifier;
	protected final Monitor monitor;
	protected final AtomicBoolean beforeSuitePublished;
	protected final AtomicBoolean afterSuitePublished;

	@Autowired
	protected DefaultMartiniSuitePreProcessorBean(EventManager eventManager, SuiteIdentifier suiteIdentifier) {
		MessageConveyor messageConveyor = new MessageConveyor(JMeterUtils.getLocale());
		logger = new LocLoggerFactory(messageConveyor).getLocLogger(getClass());
		this.eventManager = eventManager;
		this.suiteIdentifier = suiteIdentifier;
		monitor = new Monitor();
		beforeSuitePublished = new AtomicBoolean(false);
		afterSuitePublished = new AtomicBoolean(false);
	}

	@Override
	public void publishBeforeSuite() {
		try {
			monitor.enterInterruptibly();
			try {
				if (beforeSuitePublished.compareAndSet(false, true)) {
					logger.debug(BEFORE_SUITE);
					eventManager.publishBeforeSuite(this, suiteIdentifier);
				}
				else {
					logger.warn(BEFORE_SUITE_ALREADY_PUBLISHED);
				}
			}
			finally {
				monitor.leave();
			}
		}
		catch (InterruptedException e) {
			logger.warn(INTERRUPTED, e);
		}
	}

	@Override
	public void publishAfterSuite() {
		try {
			monitor.enterInterruptibly();
			try {
				if (!beforeSuitePublished.get()) {
					logger.warn(BEFORE_SUITE_NOT_PUBLISHED);
				}
				else if (afterSuitePublished.compareAndSet(false, true)) {
					logger.info(AFTER_SUITE);
					eventManager.publishAfterSuite(this, suiteIdentifier);
				}
			}
			finally {
				monitor.leave();
			}
		}
		catch (InterruptedException e) {
			logger.warn(INTERRUPTED, e);
		}
	}

	@Override
	public void destroy() {
		publishAfterSuite();
	}
}
