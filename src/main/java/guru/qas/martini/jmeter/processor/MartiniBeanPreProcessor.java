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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.MartiniException;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.I18n;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

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

	// TODO: the right thing when an error is encountered

	private static final long serialVersionUID = 7661543131018896932L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanPreProcessor.class);

	protected static final String PROPERTY_BEAN_NAME = "martini.bean.preprocessor.bean.name";
	protected static final String PROPERTY_BEAN_TYPE = "martini.bean.preprocessor.bean.type";
	protected static final String PROPERTY_ON_ERROR = "martini.bean.preprocessor.on.error";

	public static final String KEY_THIS = "martini.bean.preprocessor";

	protected volatile transient PreProcessor bean;
	protected volatile transient NoThreadClone asNoThreadClone;
	protected volatile transient LoopIterationListener asLoopIterationListener;
	protected volatile transient TestIterationListener asTestIterationListener;
	protected volatile transient TestStateListener asTestStateListener;
	protected volatile transient ThreadListener asThreadListener;
	protected volatile transient AtomicReference<Exception> exceptionRef;

	public MartiniBeanPreProcessor() {
		super();
		exceptionRef = new AtomicReference<>();
	}

	protected Object readResolve() {
		this.exceptionRef = new AtomicReference<>();
		return this;
	}

	@Override
	public Object clone() {
		MartiniBeanPreProcessor clone = MartiniBeanPreProcessor.class.cast(super.clone());
		clone.exceptionRef = exceptionRef;
		try {
			if (null != asNoThreadClone) {
				setMembers(clone);
			}
			else if (Cloneable.class.isInstance(bean)) {
				setClonedBean(clone);
			}
			else if (null != bean) {
				String message = I18n.getMessage(getClass(), "error.bean.implementation", bean.getClass());
				throw new MartiniException(message);
			}
		}
		catch (MartiniException e) {
			exceptionRef.compareAndSet(null, e);
		}
		catch (Exception e) {
			String message = I18n.getMessage(getClass(), "error.during.clone", null == bean ? null : bean.getClass());
			exceptionRef.compareAndSet(null, e);
		}

		return clone;
	}

	protected void setMembers(MartiniBeanPreProcessor clone) {
		clone.bean = bean;
		clone.asNoThreadClone = asNoThreadClone;
		clone.asLoopIterationListener = asLoopIterationListener;
		clone.asTestIterationListener = asTestIterationListener;
		clone.asTestStateListener = asTestStateListener;
		clone.asThreadListener = asThreadListener;
		clone.exceptionRef = exceptionRef;
	}

	protected void setClonedBean(MartiniBeanPreProcessor clone) throws Exception {
		Class<? extends PreProcessor> implementation = bean.getClass();
		Method method = implementation.getMethod("clone");
		Object o = method.invoke(bean);
		if (!PreProcessor.class.isInstance(o)) {
			String message = I18n.getMessage(getClass(), "error.clone.not.preprocessor", null == o ? null : o.getClass());
			throw new MartiniException(message);
		}
		clone.cast(o);
	}

	public void setBeanName(String s) {
		super.setProperty(PROPERTY_BEAN_NAME, null == s ? null : s.trim());
	}

	public String getBeanName() {
		return getNormalized(PROPERTY_BEAN_NAME);
	}

	protected String getNormalized(String propertyName) {
		String property = getPropertyAsString(propertyName);
		String trimmed = null == property ? "" : property.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public void setBeanType(String s) {
		super.setProperty(PROPERTY_BEAN_TYPE, null == s ? null : s.trim());
	}

	public String getBeanType() {
		return getNormalized(PROPERTY_BEAN_TYPE);
	}

	public void setOnError(OnError a) {
		if (null == a) {
			super.removeProperty(PROPERTY_ON_ERROR);
		}
		else {
			super.setProperty(PROPERTY_ON_ERROR, a.name());
		}
	}

	public OnError getOnError() {
		String name = super.getPropertyAsString(PROPERTY_ON_ERROR, null);
		return null == name ? OnError.STOP_TEST : OnError.valueOf(name);
	}

	@Override
	public void testStarted() {
		testStarted(null);
	}

	@Override
	public void testStarted(String host) {
		try {
			initializeDelegate();
			if (null != asTestStateListener) {
				if (null == host) {
					asTestStateListener.testStarted();
				}
				else {
					asTestStateListener.testStarted(host);
				}
			}
		}
		catch (MartiniException e) {
			exceptionRef.compareAndSet(null, e);
			Gui.reportError(this, e);
		}
		catch (Exception e) {
			exceptionRef.compareAndSet(null, e);
			Gui.reportError(this, "error.unexpected.startup", e);
		}
	}

	protected void initializeDelegate() throws Exception {
		ApplicationContext springContext = getSpringContext();
		Class<? extends PreProcessor> implementation = getImplementation(springContext);
		bean = getBean(springContext, implementation);
		cast(bean);
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

	protected PreProcessor getBean(ApplicationContext springContext, Class<? extends PreProcessor> implementation) {
		String name = getBeanName();

		Object bean;
		if (null == name && null == implementation) {
			String message = I18n.getMessage(getClass(), "error.gui.provide.bean.information");
			throw new MartiniException(message);
		}
		else if (null == name) {
			bean = springContext.getBean(implementation);
		}
		else if (null == implementation) {
			bean = springContext.getBean(name);
		}
		else {
			bean = springContext.getBean(name, implementation);
		}

		assertPreProcessor(bean);
		return PreProcessor.class.cast(bean);
	}

	protected void assertPreProcessor(Object bean) {
		if (!PreProcessor.class.isInstance(bean)) {
			Class<?> type = bean.getClass();
			String message = I18n.getMessage(getClass(), "error.gui.incompatible.bean.type", type);
			throw new MartiniException(message);
		}
	}

	protected ApplicationContext getSpringContext() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(KEY_SPRING_CONTEXT);
		checkState(ApplicationContext.class.isInstance(o), "variable %s value %s not an instance of ApplicationContext", KEY_SPRING_CONTEXT, null == o ? null : o.getClass());
		return ApplicationContext.class.cast(o);
	}

	@SuppressWarnings("unchecked")
	protected Class<? extends PreProcessor> getImplementation(ApplicationContext springContext) throws Exception {
		Class<? extends PreProcessor> implementation = null;

		String beanName = getBeanName();
		String beanType = getBeanType();

		if (null != beanType) {
			ClassLoader classLoader = springContext.getClassLoader();
			Class<?> clazz = Class.forName(beanType, true, classLoader);
			if (!PreProcessor.class.isAssignableFrom(clazz)) {
				String message = I18n.getMessage(getClass(), "error.incompatible.class.type", clazz);
				throw new MartiniException(message);
			}
			implementation = (Class<? extends PreProcessor>) clazz;
		}
		return implementation;
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		if (null != exceptionRef.get()) {
			TestElement source = event.getSource();
			JMeterContext threadContext = source.getThreadContext();
			threadContext.getEngine().stopTest(true);
			threadContext.getThread().stop();
		}
		else if (null != asTestIterationListener) {
			asTestIterationListener.testIterationStart(event);
		}
	}

	@Override
	public void threadStarted() {
		if (null != asThreadListener) {
			asThreadListener.threadStarted();
		}
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		if (null != asLoopIterationListener) {
			asLoopIterationListener.iterationStart(event);
		}
	}

	@Override
	public void process() {
		if (null != bean) {
			bean.process();
		}
	}

	@Override
	public void threadFinished() {
		if (null != asThreadListener) {
			asThreadListener.threadFinished();
		}
	}

	@Override
	public void testEnded() {
		testEnded(null);
	}

	@Override
	public void testEnded(String host) {
		exceptionRef.set(null);
		try {
			if (null != asTestStateListener) {
				if (null == host) {
					asTestStateListener.testEnded();
				}
				else {
					asTestStateListener.testEnded(host);
				}
			}
		}
		finally {
			disposeDelegate();
			destroyDelegate();
		}
	}

	protected void disposeDelegate() {
		if (DisposableBean.class.isInstance(bean)) {
			DisposableBean disposable = DisposableBean.class.cast(bean);
			try {
				disposable.destroy();
			}
			catch (Exception e) {
				String message = I18n.getMessage(getClass(), "error.disposing.bean", bean);
				LOGGER.warn(message, e);
			}
		}
	}

	protected void destroyDelegate() {
		bean = null;
		asLoopIterationListener = null;
		asTestIterationListener = null;
		asTestStateListener = null;
		asThreadListener = null;
	}
}