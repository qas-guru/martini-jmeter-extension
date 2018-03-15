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

package guru.qas.martini.jmeter.control;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import org.apache.jmeter.control.Controller;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.threads.TestCompilerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.Striped;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.jmeter.Constants;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.Il8n;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends AbstractTestElement implements Controller, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = 2700570246170278883L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniController.class);

	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	protected transient LinkedHashSet<TestElement> subElements;
	protected transient Deque<TestElement> elementDeque;
	protected transient LinkedHashSet<LoopIterationListener> listeners;
	protected transient int subElementsLoop;

	protected volatile transient Striped<Lock> lock;
	protected volatile transient AtomicReference<Collection<Martini>> martinisRef;
	protected volatile transient Map<Integer, ConcurrentLinkedDeque<Martini>> index;
	protected volatile transient UUID id;

	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	@Override
	public Object clone() {
		MartiniController clone = MartiniController.class.cast(super.clone());
		setSharedMembers(clone);
		return clone;
	}

	protected void setSharedMembers(MartiniController clone) {
		clone.lock = lock;
		clone.martinisRef = martinisRef;
		clone.index = index;
		clone.id = id;
	}

	@Override
	public void testStarted() {
		initializeSharedMembers();
	}

	protected void initializeSharedMembers() {
		lock = Striped.lock(2);
		martinisRef = new AtomicReference<>();
		index = new HashMap<>();
		id = UUID.randomUUID();
	}

	@Override
	public void setRunningVersion(boolean runningVersion) {
		super.setRunningVersion(runningVersion);
		if (runningVersion) {
			initializeLocalMembers();
		}
		else {
			destroyLocalMembers();
		}
	}

	protected void initializeLocalMembers() {
		subElements = new LinkedHashSet<>();
		elementDeque = new ArrayDeque<>();
		listeners = new LinkedHashSet<>();
	}

	protected void destroyLocalMembers() {
		subElements = null;
		elementDeque = null;
		listeners = null;
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void addTestElement(TestElement element) {
		addTestElementOnce(element);
	}

	@Override
	public boolean addTestElementOnce(TestElement element) {
		boolean evaluation = false;
		if (null != subElements && (Sampler.class.isInstance(element) || Controller.class.isInstance(element))) {
			evaluation = subElements.add(element);
		}
		return evaluation;
	}

	@Override
	public void addIterationListener(LoopIterationListener listener) {
		if (null != listeners) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeIterationListener(LoopIterationListener listener) {
		if (null != listeners) {
			listeners.remove(listener);
		}
	}

	@Override
	public void initialize() { // Never called.
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		int iteration = event.getIteration();
		if (1 == iteration) {
			initializeMartinis();
		}

		ConcurrentLinkedDeque<Martini> deque = getMartiniDeque(iteration);
		setMartiniDequeVariable(event, deque);
		subElementsLoop = 0;
		cueNextMartini();
	}

	protected void initializeMartinis() {
		Exception exception = null;

		Lock refLock = lock.get(martinisRef);
		refLock.lock();
		try {
			if (null == martinisRef.get()) {
				Collection<Martini> martinis = getMartinis();
				martinisRef.set(martinis);
			}
		}
		catch (Exception e) {
			exception = e;
			martinisRef.compareAndSet(null, Collections.emptyList());
		}
		finally {
			refLock.unlock();
		}

		if (MartiniException.class.isInstance(exception)) {
			Gui.getInstance().reportError(getClass(), MartiniException.class.cast(exception));
		}
		else if (Exception.class.isInstance(exception)) {
			String name = getName();
			MartiniException e = getException("error.retrieving.scenarios", exception, name);
			Gui.getInstance().reportError(getClass(), e);
		}
	}

	protected ConcurrentLinkedDeque<Martini> getMartiniDeque(int iteration) {
		ConcurrentLinkedDeque<Martini> deque;
		Lock indexLock = lock.get(index);
		indexLock.lock();
		try {
			deque = index.get(iteration);
			if (null == deque) {
				Collection<Martini> martinis = martinisRef.get();
				deque = new ConcurrentLinkedDeque<>(martinis);
				index.put(iteration, deque);
			}
		}
		finally {
			indexLock.unlock();
		}
		return deque;
	}

	protected Collection<Martini> getMartinis() throws MartiniException {
		JMeterContext threadContext = super.getThreadContext();
		ApplicationContext springContext = getSpringContext(threadContext);
		return getMartinis(springContext);
	}

	protected ApplicationContext getSpringContext(JMeterContext threadContext) throws MartiniException {
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(Constants.KEY_SPRING_CONTEXT);
		if (!ApplicationContext.class.isInstance(o)) {
			throwMartiniException("warning.spring.context.not.set", getName());
		}
		return ApplicationContext.class.cast(o);
	}

	protected void throwMartiniException(String key, Object... arguments) {
		throw getException(key, null, arguments);
	}

	protected MartiniException getException(String key, Exception cause, Object... arguments) {
		Il8n il8n = Il8n.getInstance();
		String message = il8n.getInterpolatedMessage(getClass(), key, arguments);
		return new MartiniException(message, cause);
	}

	protected Collection<Martini> getMartinis(ApplicationContext springContext) throws MartiniException {
		Mixologist mixologist = springContext.getBean(Mixologist.class);
		String filter = getSpelFilter();
		Collection<Martini> collection =
			filter.isEmpty() ? mixologist.getMartinis() : mixologist.getMartinis(filter);

		if (collection.isEmpty() && filter.isEmpty()) {
			throwMartiniException("warning.no.martinis.found", getName());
		}
		else if (collection.isEmpty()) {
			throwMartiniException("warning.no.martinis.match.filter", filter, getName());
		}
		return collection;
	}

	protected void setMartiniDequeVariable(LoopIterationEvent event, ConcurrentLinkedDeque<Martini> deque) {
		TestElement source = event.getSource();
		JMeterContext threadContext = source.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		String key = String.format("martini.%s.deque", id);
		variables.putObject(key, deque);
	}

	protected void cueNextMartini() {
		Deque<Martini> deque = getMartiniDequeVariable();
		Martini martini = deque.poll();
		setMartiniVariable(martini);

		if (null != martini) {
			sendLoopIterationEvent();
			elementDeque = new ArrayDeque<>(subElements);
			advanceElementDeque();
		}
	}

	@SuppressWarnings("unchecked")
	protected Deque<Martini> getMartiniDequeVariable() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		String key = String.format("martini.%s.deque", id);
		Object o = variables.getObject(key);

		Deque<Martini> deque = null;
		if (Deque.class.isInstance(o)) {
			deque = (Deque<Martini>) o;
		}
		else {
			LOGGER.warn("variable {} not set", key);
		}
		return null == deque ? new ArrayDeque<>() : deque;
	}

	protected void setMartiniVariable(Martini martini) {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		String key = String.format("martini.%s", id);
		variables.putObject(key, martini);
	}

	protected void sendLoopIterationEvent() {
		if (!listeners.isEmpty()) {
			LoopIterationEvent event = new LoopIterationEvent(this, ++subElementsLoop);
			listeners.forEach(l -> l.iterationStart(event));
		}
	}

	protected TestElement advanceElementDeque() {
		TestElement peek = elementDeque.peek();
		while (Controller.class.isInstance(peek) && Controller.class.cast(peek).isDone()) {
			elementDeque.pop();
			peek = elementDeque.peek();
		}
		return peek;
	}

	@Override
	public boolean isDone() {
		return false; // Returning true would remove this controller from the parent's subtree.
	}

	@SuppressWarnings("unchecked")
	@Override
	public Sampler next() {
		Sampler sampler = cueNextSampler();
		if (null == sampler) {
			cueNextMartini();
			sampler = cueNextSampler();
		}
		return sampler;
	}

	protected Sampler cueNextSampler() {
		Sampler sampler = null;
		TestElement peek = advanceElementDeque();
		if (Controller.class.isInstance(peek)) {
			Controller subController = Controller.class.cast(peek);
			sampler = subController.next();
		}
		else if (null != peek) {
			TestElement pop = elementDeque.pop();
			if (Sampler.class.isInstance(pop)) {
				sampler = Sampler.class.cast(pop);
			}
			else {
				LOGGER.warn("TestElement is not an instance of Controller or Sampler: {}", pop);
			}
		}
		return sampler;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void triggerEndOfLoop() {
	}

	@Override
	public void testEnded() {
		releaseMartinis();
		releaseIndex();
	}

	protected void releaseMartinis() {
		Collection<Martini> martinis = null == martinisRef ? null : martinisRef.get();
		if (null != martinis) {
			martinis.clear();
		}
	}

	protected void releaseIndex() {
		if (null != index) {
			index.values().forEach(v -> {
				if (null != v) {
					v.clear();
				}
			});
			index.clear();
		}
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}
