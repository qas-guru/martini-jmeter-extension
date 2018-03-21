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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import com.google.common.util.concurrent.Striped;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Constants;
import guru.qas.martini.jmeter.Gui;

import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

/**
 * A custom JMeter Controller class that loads Martinis from a Spring ApplicationContext and repeatedly all subelements
 * with a single Martini per thread until its queue is drained.
 * <p>
 * The correct operation of this Controller relies on a MartiniSpringPreprocessor being configured at
 * an ancestor level as it references an ApplicationContext in thread variables.
 * <p>
 * Note that this is in effect a looping controller that will remove one element from a queue per parent loop
 * and execute all descendant Samplers, populating each Sampler's SamplerContext with a Martini under the key
 * martini.current.
 * <p>
 * This may have some confusing effects. For example:
 * <ul>
 * <li>If you have more threads assigned than available Martinis, the number of loops will be
 * equivalent to the number of Martinis and some threads will not execute a sampler. Ten threads for
 * four martinis will result in four sub-element loops.
 * </li>
 * <li>If you have fewer threads assigned than available Martinis, the threads will repeatedly loop
 * over subelements until the Martinis are drained. Five threads for ten tests will result in two
 * loops by each thread.
 * </li>
 * <li>If you have five samplers for a single controller, each of the five samplers will be executed
 * with the same Martini.  If you were to set the parent's loop to a value of 3, then a successful
 * test will produce 15 samples.
 * </li>
 * <li>If no executable Martinis are found, an error will be reported through the GUI but the remainder
 * of the Test Plan will still execute.</li>
 * <li>
 * Only Controllers and Samplers are accepted as subelements of this Controller. If a subclass of this controller allows
 * any other type of element into the subtree, it will create a test failure and subsequently re-start the subelement execution
 * or continue with the next subelement depending on the configuration of the ThreadGroup.
 * </li>
 * </ul>
 */
@SuppressWarnings("WeakerAccess")
public class MartiniController extends AbstractTestElement implements Controller, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = 2700570246170278883L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniController.class);

	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	protected transient MessageSource messageSource;
	protected transient LinkedHashSet<TestElement> subElements;
	protected transient Deque<TestElement> elementDeque;
	protected transient LinkedHashSet<LoopIterationListener> listeners;
	protected transient int subElementsLoop;

	protected volatile transient Striped<Lock> lock;
	protected volatile transient AtomicReference<Collection<Martini>> martinisRef;
	protected volatile transient ConcurrentHashMap<Integer, ConcurrentLinkedDeque<Martini>> index;
	protected transient UUID id;

	public MartiniController() {
		init();
	}

	protected void init() {
		messageSource = MessageSources.getMessageSource(getClass());
		subElements = new LinkedHashSet<>();
		elementDeque = new ArrayDeque<>();
		listeners = new LinkedHashSet<>();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	/**
	 * Called by JMeter GUI.
	 *
	 * @param spelFilter Spring filter used to limit selected Martini Scenarios.
	 */
	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	/**
	 * Called by JMeter GUI.
	 *
	 * @return currently configured filter.
	 */
	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Frequently called by JMeter, both during GUI modifications and during test lifecycle.
	 * <p>
	 * Note that clone() will be called in creation of Controllers at the beginning of a
	 * test run and for each subsequent thread.
	 *
	 * @return a copy of this Controller with shared members populated
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted() {
		initializeSharedMembers();
	}

	protected void initializeSharedMembers() {
		lock = Striped.lock(100);
		martinisRef = new AtomicReference<>();
		index = new ConcurrentHashMap<>();
		id = UUID.randomUUID();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(String host) {
		testStarted();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Called by JMeter at beginning of test run to add child test elements.
	 * Attempting to call this yourself outside the JMeter compile phase will result in exceptions.
	 *
	 * @param element to be added to test tree
	 */
	@Override
	public void addTestElement(TestElement element) {
		addTestElementOnce(element);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Appears to have been added to compensate for poor memory handling.
	 */
	@Override
	public boolean addTestElementOnce(TestElement element) {
		boolean evaluation = false;
		if (null != subElements && (Sampler.class.isInstance(element) || Controller.class.isInstance(element))) {
			evaluation = subElements.add(element);
		}
		return evaluation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addIterationListener(LoopIterationListener listener) {
		if (null != listeners) {
			listeners.add(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeIterationListener(LoopIterationListener listener) {
		if (null != listeners) {
			listeners.remove(listener);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This doesn't appear to be called by the JMeter engine.
	 */
	@Override
	public void initialize() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * At this point in the lifecycle, the MartiniSpringPreProcessor has loaded Spring
	 * and set the ApplicationContext as a thread variable.
	 * </p>
	 * Note that this method may be called multiple times, once per thread.
	 */
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

	/**
	 * Blocking method, allows first thread in to load Martini selection.
	 * Configuration exceptions will result in an empty collection being loaded to avoid test plan interruption.
	 * Any other exception will cause the test to halt.
	 * All exceptions will be reported once.
	 */
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
		catch (MartiniException e) {
			exception = e;
		}
		catch (Exception e) {
			super.getThreadContext().getEngine().stopTest(true);
			exception = e;
		}
		finally {
			martinisRef.compareAndSet(null, Collections.emptyList());
			refLock.unlock();
		}

		if (MartiniException.class.isInstance(exception)) {
			LOGGER.warn(exception.getMessage());
			Gui.reportError(this, MartiniException.class.cast(exception));
		}
		else if (Exception.class.isInstance(exception)) {
			MartiniException martiniException = getExceptionBuilder()
				.setCause(exception)
				.setKey("error.retrieving.scenarios")
				.build();
			LOGGER.error(martiniException.getMessage(), exception);
			Gui.reportError(this, martiniException);
		}
	}

	protected MartiniException.Builder getExceptionBuilder() {
		return new MartiniException.Builder()
			.setMessageSource(messageSource)
			.setLocale(JMeterUtils.getLocale());
	}

	/**
	 * Sets Martini scenarios to be executed for the current loop. This is shared by threads in the parent
	 * ThreadGroup; multiple threads will drain the deque. If one of the threads encounters an exception and
	 * the ThreadGroup is configured to continue to the next loop, other threads working on the given loop can
	 * continue draining this loop's que.
	 *
	 * @param iteration loop number for this Controller, typically set by ThreadGroup or looping parent Controller
	 * @return set of Martini scenarios to be executed for the given iteration
	 */
	protected ConcurrentLinkedDeque<Martini> getMartiniDeque(int iteration) {
		ConcurrentLinkedDeque<Martini> deque;
		Lock iterationLock = lock.get(iteration);
		iterationLock.lock();
		try {
			deque = index.get(iteration);
			if (null == deque) {
				Collection<Martini> martinis = martinisRef.get();
				deque = new ConcurrentLinkedDeque<>(martinis);
				index.put(iteration, deque);
			}
		}
		finally {
			iterationLock.unlock();
		}
		return deque;
	}

	protected Collection<Martini> getMartinis() throws MartiniException {
		JMeterContext threadContext = super.getThreadContext();
		ApplicationContext springContext = getSpringContext(threadContext);
		return getMartinis(springContext);
	}

	/**
	 * Retrieves the Spring ApplicationContext from thread variables. For this to work, a MartiniSpringPreProcessor
	 * should be configured at the TestPlan level.
	 *
	 * @param threadContext same whether obtained from an event, sampler or super.getThreadContext()
	 * @return Spring ApplicationContext
	 * @throws MartiniException if no such variable has been set, or is set to something other than an ApplicationContext
	 */
	protected ApplicationContext getSpringContext(JMeterContext threadContext) throws MartiniException {
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(KEY_SPRING_CONTEXT);
		if (!ApplicationContext.class.isInstance(o)) {
			throw getExceptionBuilder()
				.setKey("error.spring.context.not.set")
				.setArguments(KEY_SPRING_CONTEXT)
				.build();
		}
		return ApplicationContext.class.cast(o);
	}

	/**
	 * @param springContext a refreshed ApplicationContext
	 * @return all Martini scenarios, or those matching the filter configured for this Controller
	 * @throws MartiniException if no scenarios were loaded
	 */
	protected Collection<Martini> getMartinis(ApplicationContext springContext) throws MartiniException {
		Mixologist mixologist = springContext.getBean(Mixologist.class);
		String filter = getSpelFilter();
		Collection<Martini> collection =
			filter.isEmpty() ? mixologist.getMartinis() : mixologist.getMartinis(filter);

		if (collection.isEmpty() && filter.isEmpty()) {
			throw getExceptionBuilder()
				.setKey("warning.no.martinis.found")
				.build();
		}
		else if (collection.isEmpty()) {
			throw getExceptionBuilder()
				.setKey("warning.no.martinis.match.filter")
				.setArguments(filter)
				.build();
		}
		return collection;
	}

	/**
	 * Sets the Martini que as a thread variable.
	 * <p>
	 * The queue must be set with a unique ID shared by all clones of this Controller. This ensures that
	 * threads traversing this Controller's samplers as well as another Martini Controller's samplers will
	 * be selecting Martinis from the correct basket. If the ID is not unique, the wrong number of
	 * samplers may be executed.
	 *
	 * @param event from which the thread context will be retrieved and on which the variable will be set
	 * @param deque the set of Martini scenarios to be executed for the given iteration
	 */
	protected void setMartiniDequeVariable(LoopIterationEvent event, ConcurrentLinkedDeque<Martini> deque) {
		TestElement source = event.getSource();
		JMeterContext threadContext = source.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		String key = getMartiniDequeKey();
		variables.putObject(key, deque);
	}

	protected String getMartiniDequeKey() {
		return String.format("martini.%s.deque", id);
	}

	/**
	 * Loads the next Martini to execute and resets the subelement que.
	 */
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

	/**
	 * Retrieves the Martini que from thread variables.
	 * If the que hasn't been set in the variables, will log a warning and continue execution for the
	 * rest of the Test plan.
	 *
	 * @return set of Martini scenarios for the current iteration
	 */
	@SuppressWarnings("unchecked")
	protected Deque<Martini> getMartiniDequeVariable() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		String key = getMartiniDequeKey();
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
		String key = getMartiniKey();
		variables.putObject(key, martini);
	}

	protected String getMartiniKey() {
		return String.format("martini.%s", id);
	}

	/**
	 * Alerts subelements we're about to iterate over them again.
	 */
	protected void sendLoopIterationEvent() {
		if (!listeners.isEmpty()) {
			LoopIterationEvent event = new LoopIterationEvent(this, ++subElementsLoop);
			listeners.forEach(l -> l.iterationStart(event));
		}
	}

	/**
	 * Walks through exhausted subelement Controllers.
	 *
	 * @return next TestElement that is not a Controller, or is not an exhausted Controller
	 */
	protected TestElement advanceElementDeque() {
		TestElement peek = elementDeque.peek();
		while (Controller.class.isInstance(peek) && Controller.class.cast(peek).isDone()) {
			elementDeque.pop();
			peek = elementDeque.peek();
		}
		return peek;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returning true effectively removes this controller from the parent's test tree.
	 */
	@Override
	public boolean isDone() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns a Sampler as long as one's configured as a sub element and the Martini deque hasn't been
	 * exhausted.
	 * <p>
	 * When the subelement deque has been exhausted and a Martini is available, the subelement deque will be
	 * reinitialized and executed with the martini.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Sampler next() {
		Sampler sampler = cueNextSampler();
		if (null == sampler) {
			cueNextMartini();
			sampler = cueNextSampler();
		}
		updateSamplerContext(sampler);
		return sampler;
	}

	protected void updateSamplerContext(Sampler sampler) {
		if (null != sampler) {
			JMeterContext threadContext = sampler.getThreadContext();
			Martini martini = getMartiniVariable(threadContext);
			Map<String, Object> samplerContext = threadContext.getSamplerContext();
			samplerContext.put(Constants.KEY_CURRENT_MARTINI, martini);
		}
	}

	protected Martini getMartiniVariable(JMeterContext context) {
		String key = getMartiniKey();
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(key);
		return Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
	}

	/**
	 * Loads the next available Sampler with leniency for non-Controller/non-Sampler elements. Where a
	 * non-Controller/non-Sampler element is encountered, it will effectively terminate iteration over
	 * subelements.
	 *
	 * @return the next available Sampler
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void triggerEndOfLoop() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Releases memory consumed by shared members.
	 */
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