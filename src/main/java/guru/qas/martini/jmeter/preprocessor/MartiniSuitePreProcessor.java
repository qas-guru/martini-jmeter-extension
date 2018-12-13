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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import guru.qas.martini.jmeter.Variables;
import guru.qas.martini.runtime.event.EventManager;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.preprocessor.MartiniSuitePreProcessorMessages.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSuitePreProcessor extends AbstractPreProcessor
	implements Serializable, Cloneable, TestBean{

	private static final long serialVersionUID = -3444643765535879540L;

	// Shared
	protected transient AtomicBoolean publishedEnd;
	protected transient EventManager eventManager;
	protected transient JMeterSuiteIdentifier suiteIdentifier;

	public MartiniSuitePreProcessor() {
		super();
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() {
		return new MartiniSuitePreProcessorBeanInfo();
	}

	@Override
	protected void completeSetup() {
		publishedEnd = new AtomicBoolean(false);
		ClassPathXmlApplicationContext springContext = getSpringContext();
		eventManager = springContext.getBean(EventManager.class);
		suiteIdentifier = JMeterSuiteIdentifier.getInstance(springContext);
		eventManager.publishBeforeSuite(this, suiteIdentifier);
	}

	protected ClassPathXmlApplicationContext getSpringContext() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(Variables.SPRING_APPLICATION_CONTEXT);
		checkState(ApplicationContext.class.isInstance(o),
			messageConveyor.getMessage(SPRING_APPLICATION_CONTEXT_UNAVAILABLE));
		return ClassPathXmlApplicationContext.class.cast(o);
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniSuitePreProcessor clone = MartiniSuitePreProcessor.class.cast(o);
		clone.publishedEnd = publishedEnd;
		clone.eventManager = eventManager;
		clone.suiteIdentifier = suiteIdentifier;
		return clone;
	}

	@Override
	public void process() {
	}

	@Override
	protected void beginTearDown() {
		if (null != publishedEnd && null != eventManager && null != suiteIdentifier) {
			if (publishedEnd.compareAndSet(false, true)) {
				eventManager.publishAfterSuite(this, suiteIdentifier);
			}
		}
		publishedEnd = null;
		suiteIdentifier = null;
		eventManager = null;
	}
}