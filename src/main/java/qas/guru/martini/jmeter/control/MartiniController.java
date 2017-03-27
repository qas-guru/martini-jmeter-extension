/*
Copyright 2017 Penny Rohr Curich

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

package qas.guru.martini.jmeter.control;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import qas.guru.martini.MartiniConstants;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements Serializable, TestStateListener, TestIterationListener {

	private static final long serialVersionUID = -597642778070067543L;

	protected static final String PROPERTY_FILTER = "spelFilter";

	protected volatile transient Monitor monitor;
	protected volatile transient AtomicReference<ImmutableList<Martini>> martinisRef;
	protected volatile transient AtomicReference<Iterator<Martini>> iteratorRef;
	protected volatile transient Map<String, Martini> index;

	public MartiniController() {
		super();
		monitor = new Monitor(true);
		martinisRef = new AtomicReference<>();
		iteratorRef = new AtomicReference<>();
		index = new HashMap<>();
	}

	public void setFilter(String text) {
		String trimmed = null == text ? "" : text.trim();
		setProperty(PROPERTY_FILTER, trimmed);
	}

	public String getFilter() {
		return getPropertyAsString(PROPERTY_FILTER);
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniController clone = MartiniController.class.cast(o);
		clone.monitor = monitor;
		clone.martinisRef = martinisRef;
		clone.iteratorRef = iteratorRef;
		clone.index = index;
		return clone;
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		monitor.enter();
		try {
			ImmutableList<Martini> martinis = martinisRef.get();
			if (null == martinis) {
				TestElement source = event.getSource();
				initializeMartinis(source);
				resetIterator();
				index.clear();
			}
		}
		finally {
			monitor.leave();
		}
	}

	protected void initializeMartinis(TestElement source) {
		JMeterContext threadContext = source.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		JMeterProperty property = threadGroup.getProperty(MartiniConstants.PROPERTY_SPRING_CONTEXT);
		Object o = null == property ? null : property.getObjectValue();
		if (!ApplicationContext.class.isInstance(o)) {
			String message =
				String.format("%s unable to find Spring application context in ThreadGroup properties", getName());
			getLogger().error(message);
			super.setDone(true);
		}
		else {
			ApplicationContext applicationContext = ApplicationContext.class.cast(o);
			initializeMartinis(applicationContext);
		}
	}

	protected Logger getLogger() {
		String implementation = getClass().getName();
		return LoggingManager.getLoggerFor(implementation);
	}

	protected void initializeMartinis(ApplicationContext context) {
		Collection<Martini> martinis = null;
		try {
			Mixologist mixologist = context.getBean(Mixologist.class);
			String filter = getFilter();
			martinis = filter.isEmpty() ? mixologist.getMartinis() : mixologist.getMartinis(filter);
		}
		catch (Exception e) {
			String message = String.format("%s unable to obtain Martini objects from Spring application context", getName());
			getLogger().error(message, e);
		}
		martinisRef.set(null == martinis ? ImmutableList.of() : ImmutableList.copyOf(martinis));
	}

	protected void resetIterator() {
		ImmutableList<Martini> martinis = martinisRef.get();
		UnmodifiableIterator<Martini> iterator = martinis.iterator();
		iteratorRef.set(iterator);
	}

	@Override
	public void testStarted() {
	}

	@Override
	public void testStarted(String host) {
	}

	protected TestElement getCurrentElement() throws NextIsNullException {
		JMeterContext threadContext = super.getThreadContext();
		JMeterThread thread = threadContext.getThread();
		String threadName = thread.getThreadName();

		JMeterVariables variables = threadContext.getVariables();
		String key = String.format("martini.%s", threadName);

		TestElement element;
		monitor.enter();
		try {
			element = super.getCurrentElement();
			Iterator<Martini> iterator = iteratorRef.get();
			boolean hasNext = iterator.hasNext();
			Martini martini = index.get(key);

			//noinspection StatementWithEmptyBody
			if (null != element && null != martini) {
				// no-op, maintain status quo
			}
			else if (null != element && hasNext) {
				martini = iterator.next();
				index.put(key, martini);
			}
			else if (null != element) {
				element = null;
			}
			else if (hasNext) {
				reInitialize();
				initializeSubControllers();
				element = super.getCurrentElement();
				martini = iterator.next();
				index.put(key, martini);
			}
			else {
				index.remove(key);
			}

			if (null == element || null == martini) {
				variables.remove(key);
			}
			else {
				variables.putObject(key, martini);
			}
		}
		finally {
			monitor.leave();
		}
		return element;
	}

	@Override
	public void testEnded() {
		martinisRef.set(null);
		iteratorRef.set(null);
	}

	@Override
	public void testEnded(String host) {
	}
}