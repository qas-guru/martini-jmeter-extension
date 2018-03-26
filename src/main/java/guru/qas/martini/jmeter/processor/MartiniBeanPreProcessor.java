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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.MessageSource;

import guru.qas.martini.MartiniException;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.SpringBeanUtil;

import static guru.qas.martini.jmeter.processor.OnError.*;

/**
 * Delegates PreProcessor calls to a user-defined bean obtained from Martini's Spring context.
 * Also forwards ThreadListener, TestStateListener, TestIterationListener and LoopIterationListener
 * events to delegate.
 * <p>
 * To hook your Spring bean into the JMeter lifecycle using this class, at a minimum have your bean
 * implement org.apache.jmeter.processor.PreProcessor and either:
 * <ul>
 * <li>{@link java.lang.Cloneable java.lang.Cloneable}</li>
 * <li>{@link org.apache.jmeter.engine.util.NoThreadClone org.apache.jmeter.engine.util.NoThreadClone}</li>
 * </ul>
 * Your bean's process() method will be called before the start of each sample. If you've implemented Cloneable,
 * a clone of your bean will be made for each executing thread. If you've implemented NoThreadClone, all threads
 * will call a single instance of your bean.
 * <p>
 * You may also implement:
 * <ul>
 * <li>{@link org.apache.jmeter.testelement.TestStateListener org.apache.jmeter.testelement.TestStateListener}</li>
 * <li>{@link org.apache.jmeter.testelement.TestIterationListener org.apache.jmeter.testelement.TestIterationListener}</li>
 * <li>{@link org.apache.jmeter.testelement.ThreadListener org.apache.jmeter.testelement.ThreadListener}</li>
 * <li>{@link org.apache.jmeter.engine.event.LoopIterationListener org.apache.jmeter.engine.event.LoopIterationListener}</li>
 * </ul>
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanPreProcessor extends AbstractTestElement implements PreProcessor, ThreadListener, TestStateListener, TestIterationListener, LoopIterationListener {

	private static final long serialVersionUID = 7661543131018896932L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanPreProcessor.class);

	protected static final String PROPERTY_BEAN_NAME = "martini.bean.preprocessor.bean.name";
	protected static final String PROPERTY_BEAN_TYPE = "martini.bean.preprocessor.bean.type";
	protected static final String PROPERTY_ARGUMENTS = "martini.bean.preprocessor.arguments";
	protected static final String PROPERTY_ON_ERROR = "martini.bean.preprocessor.on.error";

	public static final String KEY_THIS = "martini.bean.preprocessor";

	protected volatile transient AtomicReference<Exception> setupExceptionRef;
	protected volatile transient AtomicReference<Exception> processExceptionRef;
	protected transient MessageSource messageSource;

	protected volatile transient PreProcessor bean;
	protected volatile transient NoThreadClone asNoThreadClone;
	protected volatile transient LoopIterationListener asLoopIterationListener;
	protected volatile transient TestIterationListener asTestIterationListener;
	protected volatile transient TestStateListener asTestStateListener;
	protected volatile transient ThreadListener asThreadListener;

	public MartiniBeanPreProcessor() {
		super();
		init();
	}

	protected void init() {
		messageSource = MessageSources.getMessageSource(getClass());
		setupExceptionRef = new AtomicReference<>();
		processExceptionRef = new AtomicReference<>();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	@Override
	public void setProperty(String name, String value) {
		String normalized = getNormalized(value);
		if (null == normalized) {
			super.removeProperty(name);
		}
		else {
			super.setProperty(name, value);
		}
	}

	protected String getNormalized(String s) {
		String trimmed = null == s ? null : s.trim();
		return null == trimmed || trimmed.isEmpty() ? null : trimmed;
	}

	public void setBeanName(String s) {
		setProperty(PROPERTY_BEAN_NAME, s);
	}

	public String getBeanName() {
		String property = getPropertyAsString(PROPERTY_BEAN_NAME);
		return getNormalized(property);
	}

	public void setBeanType(String s) {
		setProperty(PROPERTY_BEAN_TYPE, s);
	}

	public String getBeanType() {
		return super.getPropertyAsString(PROPERTY_BEAN_TYPE);
	}

	public void setOnError(OnError a) {
		setProperty(PROPERTY_ON_ERROR, null == a ? null : a.name());
	}

	public OnError getOnError() {
		String name = super.getPropertyAsString(PROPERTY_ON_ERROR, null);
		return null == name ? STOP_TEST : OnError.valueOf(name);
	}

	public Arguments getArguments() {
		JMeterProperty property = super.getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : new Arguments();
	}

	public void setArguments(Arguments arguments) {
		ObjectProperty property = new ObjectProperty(PROPERTY_ARGUMENTS, arguments);
		super.setProperty(property);
	}

	@Override
	public Object clone() {
		MartiniBeanPreProcessor clone = MartiniBeanPreProcessor.class.cast(super.clone());
		clone.init();
		clone.setupExceptionRef = setupExceptionRef;
		clone.messageSource = messageSource;
		executeSetup(() -> {
			if (null != asNoThreadClone) {
				clone.processExceptionRef = processExceptionRef;
				clone.cast(bean);
			}
			else if (Cloneable.class.isInstance(bean)) {
				Object o = cloneDelegate();
				assertCloneIsPreProcessor(o);
				clone.cast(o);
			}
			else if (null != bean) {
				throw getExceptionBuilder()
					.setKey("error.bean.implementation")
					.setArguments(bean.getClass())
					.build();
			}
		});
		return clone;
	}

	protected MartiniException.Builder getExceptionBuilder() {
		return new MartiniException.Builder()
			.setLocale(JMeterUtils.getLocale())
			.setMessageSource(messageSource);
	}

	protected Object cloneDelegate() {
		try {
			Class<?> implementation = bean.getClass();
			Method method = implementation.getMethod("clone");
			return method.invoke(bean);
		}
		catch (Exception e) {
			throw getExceptionBuilder()
				.setCause(e)
				.setKey("error.cloning.delegate")
				.setArguments(bean.getClass())
				.build();
		}
	}

	protected void assertCloneIsPreProcessor(Object o) {
		if (!PreProcessor.class.isInstance(o)) {
			throw getExceptionBuilder()
				.setKey("error.clone.not.preprocessor")
				.setArguments(null == o ? null : o.getClass())
				.build();
		}
	}

	@Override
	public void testStarted() {
		testStarted(null);
	}

	@Override
	public void testStarted(String host) {
		setupExceptionRef = new AtomicReference<>();
		processExceptionRef = new AtomicReference<>();

		if (initializeDelegate() && null != asTestStateListener) {
			executeSetup(() -> {
				if (null == host) {
					asTestStateListener.testStarted();
				}
				else {
					asTestStateListener.testStarted(host);
				}
			});
		}
	}

	protected boolean initializeDelegate() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		variables.putObject(KEY_THIS, this);

		executeSetup(() -> {
			String name = getBeanName();
			String type = getBeanType();
			bean = SpringBeanUtil.getBean(name, type, PreProcessor.class);
			cast(bean);
		});
		return !isInHaltingCondition();
	}

	protected void cast(Object o) {
		bean = cast(o, PreProcessor.class);
		asNoThreadClone = cast(o, NoThreadClone.class);
		asLoopIterationListener = cast(o, LoopIterationListener.class);
		asTestIterationListener = cast(o, TestIterationListener.class);
		asTestStateListener = cast(o, TestStateListener.class);
		asThreadListener = cast(o, ThreadListener.class);
	}

	protected <T> T cast(Object o, Class<T> implementation) {
		return implementation.isInstance(o) ? implementation.cast(o) : null;
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		if (null != asTestIterationListener) {
			executeSetup(() -> asTestIterationListener.testIterationStart(event));
		}
	}

	protected void executeSetup(Runnable r) {
		execute(r, setupExceptionRef);
	}

	protected void executeProcess(Runnable r) {
		execute(r, processExceptionRef);
	}

	protected void execute(Runnable r, AtomicReference<Exception> exceptionRef) {
		try {
			r.run();
		}
		catch (Exception e) {
			exceptionRef.compareAndSet(null, e);
		}
	}

	protected void executeSilently(Runnable r) {
		try {
			r.run();
		}
		catch (Exception e) {
			LOGGER.error("encountered unexpected Exception", e);
		}
	}

	protected boolean isInHaltingCondition() {
		boolean exceptionEncountered = null != setupExceptionRef.get() || null != processExceptionRef.get();
		OnError instructions = getOnError();
		return exceptionEncountered && !PROCEED.equals(instructions);
	}

	protected void halt(TestElement source) {
		JMeterContext threadContext = source.getThreadContext();

		OnError instruction = getOnError();
		switch (instruction) {
			case STOP_TEST:
				haltTest(threadContext);
				break;
			case STOP_THREAD_GROUP:
				haltThreadGroup(threadContext);
				break;
			case STOP_THREAD:
				haltThread(threadContext);
				break;
			default:
				LOGGER.warn("Unrecognized OnError: {}; will halt test.", instruction);
				haltTest(threadContext);
		}
	}

	protected void haltTest(JMeterContext threadContext) {
		StandardJMeterEngine engine = threadContext.getEngine();
		engine.stopTest(true);
		reportHalt("halting.test.on.error");
	}

	protected void reportHalt(String key, Object... args) {
		Exception exception = setupExceptionRef.get();
		exception = null == exception ? processExceptionRef.get() : exception;
		String message = messageSource.getMessage(key, args, JMeterUtils.getLocale());
		LOGGER.error(message, exception);
		Gui.reportError(this, message, exception);
	}

	protected void haltThreadGroup(JMeterContext threadContext) {
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		threadGroup.stop();
		reportHalt("halting.thread.group.on.error", threadGroup.getName());
	}

	protected void haltThread(JMeterContext threadContext) {
		JMeterThread thread = threadContext.getThread();
		thread.stop();
		reportHalt("halting.thread.on.error", thread.getThreadName());
	}

	@Override
	public void threadStarted() {
		if (!isInHaltingCondition() && null != asThreadListener) {
			executeSetup(() -> asThreadListener.threadStarted());
		}
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		if (null != asLoopIterationListener) {
			executeSetup(() -> asLoopIterationListener.iterationStart(event));
		}
	}

	@Override
	public void process() {
		JMeterContext threadContext = super.getThreadContext();
		Sampler sampler = threadContext.getCurrentSampler();
		if (isInHaltingCondition()) {
			halt(sampler);
		}
		else if (null != bean) {
			executeProcess(() -> {
				Map<String, Object> samplerContext = threadContext.getSamplerContext();
				samplerContext.put(KEY_THIS, this);
				bean.process();
			});

			if (isInHaltingCondition()) {
				halt(sampler);
			}
		}
	}

	@Override
	public void threadFinished() {
		if (!isInHaltingCondition() && null != asThreadListener) {
			executeSilently(() -> asThreadListener.threadFinished());
		}
	}

	@Override
	public void testEnded() {
		testEnded(null);
	}

	@Override
	public void testEnded(String host) {
		if (null != asTestStateListener) {
			executeSilently(() -> {
				if (null == host) {
					asTestStateListener.testEnded();
				}
				else {
					asTestStateListener.testEnded(host);
				}
			});
		}

		disposeDelegate();
		setupExceptionRef = null;
		processExceptionRef = null;
		bean = null;
		asLoopIterationListener = null;
		asTestIterationListener = null;
		asTestStateListener = null;
		asThreadListener = null;
	}

	protected void disposeDelegate() {
		if (DisposableBean.class.isInstance(bean)) {
			DisposableBean disposable = DisposableBean.class.cast(bean);
			try {
				disposable.destroy();
			}
			catch (Exception e) {
				String message = messageSource.getMessage(
					"error.disposing.bean", new Object[]{bean.getClass()}, JMeterUtils.getLocale());
				LOGGER.warn(message, e);
			}
		}
	}
}