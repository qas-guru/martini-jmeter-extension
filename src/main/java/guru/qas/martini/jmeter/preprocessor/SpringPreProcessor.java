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
import java.util.Locale;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static guru.qas.martini.jmeter.preprocessor.SpringPreProcessorMessages.*;

/**
 * Manages a Spring ClassPathXmlApplicationContext, making the context accessible to setup and test threads
 * through JMeterVariables as SpringPreProcessor.VARIABLE "martini.spring.application.context".
 * <p>
 * One enabled SpringPreProcessor should be configured at the top-level of the test plan before
 * any ThreadGroup configurations.
 */
@SuppressWarnings("WeakerAccess")
public class SpringPreProcessor
	extends AbstractTestElement
	implements Serializable, Cloneable, PreProcessor, TestBean, TestStateListener, TestIterationListener {

	private static final long serialVersionUID = -1582951167073002597L;

	public static final String VARIABLE = "martini.spring.application.context";

	// Shared.
	protected transient ClassPathXmlApplicationContext springContext;
	protected transient LocLogger logger;

	public SpringPreProcessor() {
		super();
	}

	@Override
	public void testStarted() {
		System.out.println("Yo! testStarted()");
		setUp();
	}

	@Override
	public void testStarted(String host) {
		System.out.println("Yo! testStarted(String)");
		setUp();
	}

	protected void setUp() {
		setUpLogger();
		logger.info(STARTING, super.getName(), System.identityHashCode(this));
	}

	protected void setUpLogger() {
		Locale locale = JMeterUtils.getLocale();
		IMessageConveyor messageConveyor = new MessageConveyor(locale);
		LocLoggerFactory loggerFactory = new LocLoggerFactory(messageConveyor);
		logger = loggerFactory.getLocLogger(this.getClass());
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		SpringPreProcessor clone = SpringPreProcessor.class.cast(o);
		clone.springContext = springContext;
		return clone;
	}

	@Override
	public void process() {
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		System.out.println("testIterationStart(LoopIterationEvent)");
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
		synchronized (ApplicationContext.class) {
			if (null != springContext) {
				try {
					springContext.close();
				}
				catch (Exception e) {
					logger.warn(SPRING_CLOSE_EXCEPTION, e);
				}
			}
		}
		springContext = null;
		logger = null;
	}
}
