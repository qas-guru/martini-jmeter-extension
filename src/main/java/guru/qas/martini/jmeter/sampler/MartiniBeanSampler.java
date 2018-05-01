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

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.config.gui.SimpleConfigGui;
import org.apache.jmeter.protocol.java.config.gui.JavaConfigGui;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import guru.qas.martini.jmeter.SpringBeanUtil;
import guru.qas.martini.jmeter.config.gui.MartiniBeanConfigGui;

import static guru.qas.martini.jmeter.config.MartiniBeanConfig.*;

/**
 * Modeled after JavaSampler.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MartiniBeanSampler extends AbstractSampler implements TestStateListener, Interruptible {

	private static final long serialVersionUID = 1813073201707263835L;

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanSampler.class);

	protected static final ImmutableSet<Class<? extends AbstractConfigGui>> GUIS = ImmutableSet.of(
		MartiniBeanConfigGui.class, JavaConfigGui.class, SimpleConfigGui.class);

	private transient AtomicReference<JavaSamplerClient> delegateRef;
	private transient AtomicReference<JavaSamplerContext> contextRef;

	public MartiniBeanSampler() {
		super();
		delegateRef = new AtomicReference<>();
		contextRef = new AtomicReference<>();
		setArguments(new Arguments());
	}

	public void setArguments(Arguments arguments) {
		TestElementProperty property = new TestElementProperty(PROPERTY_ARGUMENTS, arguments);
		setProperty(property);
	}

	@Override
	public boolean applies(ConfigTestElement configElement) {
		String className = configElement.getProperty(TestElement.GUI_CLASS).getStringValue();

		boolean evaluation = false;
		try {
			Class<?> implementation = Class.forName(className, true, this.getClass().getClassLoader());
			evaluation = GUIS.stream().anyMatch(g -> g.isAssignableFrom(implementation));
		}
		catch (ClassNotFoundException e) {
			LOGGER.warn("configured GUI not available", e);
		}
		return evaluation;
	}

	public String getBeanName() {
		return getArgument(ARGUMENT_BEAN_NAME);
	}

	public void setBeanName(String s) {
		setArgument(ARGUMENT_BEAN_NAME, s);
	}

	public String getBeanType() {
		return getArgument(ARGUMENT_BEAN_TYPE);
	}

	public void setBeanType(String s) {
		setArgument(ARGUMENT_BEAN_TYPE, s);
	}

	protected String getArgument(String s) {
		Arguments arguments = getArguments();
		Map<String, String> argumentMap = arguments.getArgumentsAsMap();
		return argumentMap.get(s);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : null;
	}

	protected void setArgument(String name, String value) {
		Arguments arguments = getArguments();
		arguments.addArgument(name, value);
	}

	@Override
	public void testStarted() {
		JavaSamplerClient delegate = getDelegate();
		TestStateListener listener = getAs(TestStateListener.class, delegate);
		if (null != listener) {
			safelyRun(listener::testStarted, "unable to execute testStarted() on delegate " + delegate);
		}
	}

	@Override
	public void testStarted(String host) {
		JavaSamplerClient delegate = getDelegate();
		TestStateListener listener = getAs(TestStateListener.class, delegate);
		if (null != listener) {
			Runnable runnable = () -> listener.testStarted(host);
			safelyRun(runnable, "unable to call testStarted(String) on delegate " + delegate);
		}
	}

	protected void safelyRun(Runnable runnable, String errorMessage) {
		try {
			runnable.run();
		}
		catch (Exception e) {
			LOGGER.warn(errorMessage, e);
		}
	}

	private JavaSamplerClient getDelegate() {
		JavaSamplerClient delegate = delegateRef.get();
		if (null == delegate) {
			String beanName = getBeanName();
			String beanType = getBeanType();
			try {
				delegate = SpringBeanUtil.getBean(beanName, beanType, JavaSamplerClient.class);
			}
			catch (Exception e) {
				LOGGER.error(
					"{}\nerror creating bean specified by name {} and type {}", toString(), beanName, beanType, e);
				delegate = new ErrorSamplerClient(getClass(), toString());
			}
			delegate = delegateRef.compareAndSet(null, delegate) ? delegate : delegateRef.get();
		}
		return null == delegate ? new ErrorSamplerClient(getClass(), toString()) : delegate;
	}

	protected <T> T getAs(Class<T> implementation, Object o) {
		return implementation.isInstance(o) ? implementation.cast(o) : null;
	}

	@Override
	public SampleResult sample(Entry entry) {
		JavaSamplerClient delegate = getDelegate();
		JavaSamplerContext context = getJavaSamplerContext();

		delegate.setupTest(context);
		SampleResult result = delegate.runTest(context);
		setLabel(result);

		return result;
	}

	private JavaSamplerContext getJavaSamplerContext() {
		JavaSamplerContext context = contextRef.get();
		if (null == context) {
			Arguments args = getArguments();
			Arguments copy = Arguments.class.cast(args.clone());
			copy.addArgument(TestElement.NAME, getName());
			context = new JavaSamplerContext(args);
			context = contextRef.compareAndSet(null, context) ? context : contextRef.get();
		}
		return context;
	}

	private void setLabel(SampleResult result) {
		if (result != null && result.getSampleLabel().trim().isEmpty()) {
			result.setSampleLabel(getName());
		}
	}

	@Override
	public boolean interrupt() {
		Interruptible interruptible = getAs(Interruptible.class, delegateRef.get());
		boolean interrupted = false;
		if (null != interruptible) {
			interrupted = safelyCall(interruptible::interrupt, "unable to call interrupt() on delegate " + interruptible);
		}
		return interrupted;
	}

	protected <T> T safelyCall(Callable<T> callable, String errorMessage) {
		T evaluation = null;
		try {
			evaluation = callable.call();
		}
		catch (Exception e) {
			LOGGER.warn(errorMessage, e);
		}
		return evaluation;
	}

	@Override
	public void testEnded() {
		TestStateListener listener = getAs(TestStateListener.class, delegateRef.get());
		if (null != listener) {
			safelyRun(listener::testEnded, "unable to call testEnded() on delegate " + listener);
		}
		cleanup();
	}

	@Override
	public void testEnded(String host) {
		TestStateListener listener = getAs(TestStateListener.class, delegateRef.get());
		if (null != listener) {
			Runnable runnable = () -> listener.testEnded(host);
			safelyRun(runnable, "unable to call testEnded(String) on delegate " + listener);
		}
		cleanup();
	}

	protected void cleanup() {
		delegateRef.set(null);
		contextRef.set(null);
	}

	@Override
	public String toString() {
		String threadName = Thread.currentThread().getName();
		String address = Integer.toHexString(hashCode());
		return String.format("%s@%s-%s", threadName, address, getName());
	}
}
