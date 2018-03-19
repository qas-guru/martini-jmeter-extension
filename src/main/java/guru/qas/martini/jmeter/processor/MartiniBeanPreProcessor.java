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

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.MartiniException;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.Il8n;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanPreProcessor extends AbstractTestElement implements PreProcessor, ThreadListener, TestStateListener, TestIterationListener, LoopIterationListener {

	private static final long serialVersionUID = 7661543131018896932L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanPreProcessor.class);

	protected static final String PROPERTY_BEAN_NAME = "martini.preprocessor.bean.name";
	protected static final String PROPERTY_BEAN_TYPE = "martini.preprocessor.bean.type";

	protected volatile transient PreProcessor bean;
	protected volatile transient NoThreadClone asNoThreadClone;
	protected volatile transient LoopIterationListener asLoopIterationListener;
	protected volatile transient TestIterationListener asTestIterationListener;
	protected volatile transient TestStateListener asTestStateListener;
	protected volatile transient ThreadListener asThreadListener;

	public MartiniBeanPreProcessor() {
		super();
	}

	@Override
	public Object clone() {
		MartiniBeanPreProcessor clone = MartiniBeanPreProcessor.class.cast(super.clone());
		if (null != asNoThreadClone) {
			clone.bean = bean;
			clone.asNoThreadClone = asNoThreadClone;
			clone.asLoopIterationListener = asLoopIterationListener;
			clone.asTestIterationListener = asTestIterationListener;
			clone.asTestStateListener = asTestStateListener;
			clone.asThreadListener = asThreadListener;
		}
		else if (Cloneable.class.isInstance(bean)) {
			try {
				Class<? extends PreProcessor> implementation = bean.getClass();
				Method method = implementation.getMethod("clone");
				Object beanClone = method.invoke(bean);
				checkState(PreProcessor.class.isInstance(beanClone), "cloned bean not of type PreProcessor");
				clone.cast(beanClone);
			}
			catch (Exception e) {
				String message = Il8n.getInstance().getMessage(getClass(), "error.clone.preprocessor");
				throw new RuntimeException(message, e);
			}
		}
		return clone;
	}

	protected <T> T cast(Object o, Class<T> implementation) {
		return implementation.isInstance(o) ? implementation.cast(o) : null;
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

	@Override
	public void testStarted() {
		initializeDelegate();

		if (null != asTestStateListener) {
			asTestStateListener.testStarted();
		}
	}

	protected void initializeDelegate() {
		try {
			ApplicationContext springContext = getSpringContext();
			Class<? extends PreProcessor> implementation = getImplementation(springContext);
			PreProcessor preProcessor = getBean(springContext, implementation);
			cast(preProcessor);
		}
		catch (MartiniException e) {
			Gui.getInstance().reportError(this, e);
		}
		catch (Exception e) {
			Il8n il8n = Il8n.getInstance();
			String logMessage = il8n.getMessage(getClass(), "error.log.preprocessor.test.started");
			LOGGER.warn(logMessage, e);

			Gui.getInstance().reportError(this, "error.gui.preprocessor.test.started", e);
		}
	}

	protected void cast(Object o) {
		asNoThreadClone = cast(o, NoThreadClone.class);
		asLoopIterationListener = cast(o, LoopIterationListener.class);
		asTestIterationListener = cast(o, TestIterationListener.class);
		asTestStateListener = cast(o, TestStateListener.class);
		asThreadListener = cast(o, ThreadListener.class);
	}

	protected PreProcessor getBean(
		ApplicationContext springContext,
		Class<? extends PreProcessor> implementation
	) throws MartiniException {
		try {
			String name = getBeanName();

			Object bean;
			if (null == name && null == implementation) {
				String message = Il8n.getInstance().getMessage(getClass(), "error.gui.provide.bean.information");
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

			if (!PreProcessor.class.isInstance(bean)) {
				Class<?> type = bean.getClass();
				String message = Il8n.getInstance().getMessage(getClass(), "error.gui.incompatible.bean.type", type);
				throw new MartiniException(message);
			}
			return PreProcessor.class.cast(bean);
		}
		catch (MartiniException e) {
			LOGGER.warn(e.getMessage());
			throw e;
		}
		catch (Exception e) {
			Il8n il8n = Il8n.getInstance();
			String logMessage = il8n.getMessage(getClass(), "error.log.preprocessor.retrieval");
			LOGGER.warn(logMessage, e);

			String guiMessage = il8n.getMessage(getClass(), "error.gui.preprocessor.retrieval");
			throw new MartiniException(guiMessage, e);
		}
	}

	protected ApplicationContext getSpringContext() throws MartiniException {
		try {
			JMeterContext threadContext = super.getThreadContext();
			JMeterVariables variables = threadContext.getVariables();
			Object o = variables.getObject(KEY_SPRING_CONTEXT);
			checkState(ApplicationContext.class.isInstance(o), "variable %s value %s not an instance of ApplicationContext", KEY_SPRING_CONTEXT, null == o ? null : o.getClass());
			return ApplicationContext.class.cast(o);
		}
		catch (Exception e) {
			Il8n il8n = Il8n.getInstance();
			String logMessage = il8n.getMessage(getClass(), "error.log.retrieving.spring.context", KEY_SPRING_CONTEXT);
			LOGGER.warn(logMessage, e);

			String guiMessage = il8n.getMessage(getClass(), "error.gui.retreiving.spring.context");
			throw new MartiniException(guiMessage, e);
		}
	}

	@SuppressWarnings("unchecked")
	protected Class<? extends PreProcessor> getImplementation(ApplicationContext springContext) throws MartiniException {
		Class<? extends PreProcessor> implementation = null;
		try {
			String beanName = getBeanName();
			String beanType = getBeanType();

			if (null != beanType) {
				ClassLoader classLoader = springContext.getClassLoader();
				Class<?> clazz = Class.forName(beanType, true, classLoader);
				checkState(PreProcessor.class.isAssignableFrom(clazz),
					"class %s is not an instance of %s", clazz, PreProcessor.class);
				implementation = (Class<? extends PreProcessor>) clazz;
			}
		}
		catch (Exception e) {
			Il8n il8n = Il8n.getInstance();
			String logMessage = il8n.getMessage(getClass(), "error.log.retrieving.bean.implementation", getBeanType());
			LOGGER.warn(logMessage, e);

			String guiMessage = il8n.getMessage(getClass(), "error.gui.retrieving.bean.implementation");
			throw new MartiniException(guiMessage, e);
		}
		return implementation;
	}

	@Override
	public void testStarted(String host) {
		initializeDelegate();

		if (null != asTestStateListener) {
			asTestStateListener.testStarted(host);
		}
	}

	@Override
	public void threadStarted() {
		if (null != asThreadListener) {
			asThreadListener.threadStarted();
		}
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		if (null != asTestIterationListener) {
			asTestIterationListener.testIterationStart(event);
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
		bean.process();
	}

	@Override
	public void threadFinished() {
		if (null != asThreadListener) {
			asThreadListener.threadFinished();
		}
	}

	@Override
	public void testEnded() {
		try {
			if (null != asTestStateListener) {
				asTestStateListener.testEnded();
			}
		}
		finally {
			destroyDelegate();
		}
	}

	protected void destroyDelegate() {
		bean = null;
		asLoopIterationListener = null;
		asTestIterationListener = null;
		asTestStateListener = null;
		asThreadListener = null;
	}

	@Override
	public void testEnded(String host) {
		try {
			if (null != asTestStateListener) {
				asTestStateListener.testEnded(host);
			}
		}
		finally {
			destroyDelegate();
		}
	}
}