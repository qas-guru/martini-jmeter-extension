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

package guru.qas.martini.jmeter.processor;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.apache.jorphan.util.JMeterStopTestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.SpringBeanUtil;

import static guru.qas.martini.jmeter.config.MartiniBeanConfig.*;

@SuppressWarnings("ALL")
public class MartiniBeanPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 5753283254019680162L;

	protected transient MessageSource messageSource;
	protected transient Logger logger;
	protected transient AtomicReference<PreProcessor> delegateRef;

	public String getBeanName() {
		Arguments arguments = getArguments();
		Map<String, String> argumentMap = arguments.getArgumentsAsMap();
		return argumentMap.get(PROPERTY_BEAN_NAME);
	}

	public String getBeanType() {
		return super.getPropertyAsString(PROPERTY_BEAN_TYPE);
	}

	public void setBeanType(String s) {
		super.setProperty(PROPERTY_BEAN_TYPE, s);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : null;
	}

	public void setArguments(Arguments arguments) {
		TestElementProperty property = new TestElementProperty(PROPERTY_ARGUMENTS, arguments);
		setProperty(property);
	}

	public MartiniBeanPreProcessor() {
		super();
		init();
	}

	protected void init() {
		delegateRef = new AtomicReference<>();
		messageSource = MessageSources.getMessageSource(getClass());
		logger = LoggerFactory.getLogger(getClass());
		setArguments(new Arguments());
	}

	protected Object readResolve() {
		init();
		return this;
	}

	@Override
	public Object clone() {
		MartiniBeanPreProcessor clone = MartiniBeanPreProcessor.class.cast(super.clone());
		clone.delegateRef = delegateRef;
		return clone;
	}

	@Override
	public void testStarted() {
		initializeDelegate();
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			listener.testStarted();
		}
	}

	@Override
	public void testStarted(String host) {
		initializeDelegate();
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			listener.testStarted(host);
		}
	}

	private PreProcessor initializeDelegate() {
		PreProcessor delegate = delegateRef.get();
		if (null == delegate) {
			String beanName = getBeanName();
			String beanType = getBeanType();
			try {
				delegate = SpringBeanUtil.getBean(beanName, beanType, PreProcessor.class);
			}
			catch (Exception e) {
				String message = String.format("%s: error creating bean specified by name %s and type %s", toString(), beanName, beanType);
				throw new JMeterStopTestException(message, e);
			}

			if (NoThreadClone.class.isInstance(delegate)) {
				delegate = delegateRef.compareAndSet(null, delegate) ? delegate : delegateRef.get();
			}
			else {
				delegateRef = new AtomicReference<>(delegate);
			}
		}

		return delegate;
	}

	@Nullable
	protected <T> T getDelegateAs(Class<T> implementation) {
		PreProcessor delegate = delegateRef.get();
		return implementation.isInstance(delegate) ? implementation.cast(delegate) : null;
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		LoopIterationListener listener = getDelegateAs(LoopIterationListener.class);
		if (null != listener) {
			listener.iterationStart(event);
		}
	}

	@Override
	public void process() {
		PreProcessor delegate = delegateRef.get();
		delegate.process();
	}

	@Override
	public void testEnded() {
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			listener.testEnded();
		}
		delegateRef.set(null);
	}

	@Override
	public void testEnded(String host) {
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			listener.testEnded(host);
		}
		delegateRef.set(null);
	}
}