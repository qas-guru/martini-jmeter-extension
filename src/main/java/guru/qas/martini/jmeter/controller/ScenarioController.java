package guru.qas.martini.jmeter.controller;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.collect.Maps;

import guru.qas.martini.event.AfterScenarioEvent;
import guru.qas.martini.event.AfterSuiteEvent;
import guru.qas.martini.event.BeforeScenarioEvent;
import guru.qas.martini.event.BeforeSuiteEvent;
import guru.qas.martini.event.MartiniEventPublisher;
import guru.qas.martini.event.MartiniScenarioEvent;
import guru.qas.martini.event.MartiniSuiteEvent;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.jmeter.preprocessor.SpringPreProcessor;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.scope.MartiniScenarioScope;
import guru.qas.martini.tag.Categories;

@SuppressWarnings({"WeakerAccess"})
@Configurable
public class ScenarioController extends GenericController
	implements Serializable, Cloneable, TestStateListener, LoopIterationListener, TestBean {

	private static final long serialVersionUID = 8752644826070376949L;

	// Shared between all instances.
	private static final AtomicBoolean SUITE_STARTED = new AtomicBoolean(false);
	private static final AtomicBoolean SUITE_ENDED = new AtomicBoolean(false);

	// Shared between clones.
	protected transient SuiteIdentifier suiteIdentifier;
	protected transient MartiniEventPublisher eventPublisher;
	protected transient AtomicReference<Map.Entry<Thread, MartiniResult>> setupContextRef;
	protected transient MartiniScenarioScope scope;
	protected transient Categories categories;

	// Per-thread.
	protected transient MartiniResult martiniResult;

	@Autowired
	void set(SuiteIdentifier i) {
		this.suiteIdentifier = i;
	}

	@Autowired
	void set(MartiniEventPublisher p) {
		this.eventPublisher = p;
	}

	@Autowired
	void set(MartiniScenarioScope s) {
		this.scope = s;
	}

	@Autowired
	void set(Categories c) { this.categories = c; }

	@Override
	public Object clone() {
		ScenarioController clone = ScenarioController.class.cast(super.clone());
		clone.suiteIdentifier = suiteIdentifier;
		clone.eventPublisher = eventPublisher;
		clone.setupContextRef = setupContextRef;
		clone.scope = scope;
		clone.categories = categories;
		return clone;
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void testStarted() {
		if (!isInterrupted()) {
			SpringPreProcessor.autowire(this);

			synchronized (SUITE_STARTED) {
				if (!SUITE_STARTED.get()) {
					MartiniSuiteEvent event = new BeforeSuiteEvent(this, suiteIdentifier);
					eventPublisher.publish(event);
					SUITE_STARTED.set(true);
				}
			}

			Thread thread = Thread.currentThread();
			MartiniResult result = startScenario();
			Map.Entry<Thread, MartiniResult> entry = Maps.immutableEntry(thread, result);
			setupContextRef = new AtomicReference<>(entry);
		}
	}

	protected static boolean isInterrupted() {
		return Thread.currentThread().isInterrupted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		int iteration = event.getIteration();
		if (1 == iteration) {
			endSetupScenario();
		}
		endScenario();
		martiniResult = startScenario();
	}

	protected MartiniResult startScenario() {
		MartiniResult result = getNewMartiniResult();
		MartiniScenarioEvent martiniEvent = new BeforeScenarioEvent(this, result);
		eventPublisher.publish(martiniEvent);
		return result;
	}

	protected MartiniResult getNewMartiniResult() {
		// TODO: get martini out of variables if possible
		// TODO: add MartiniController
		// TODO: add Martini Sampler
		return DefaultMartiniResult.builder()
			.setMartiniSuiteIdentifier(suiteIdentifier)
			.setMartini(new JMeterMartini())
			.build(categories);
	}

	protected void endSetupScenario() {
		Map.Entry<Thread, MartiniResult> entry = setupContextRef.get();
		if (null != entry && setupContextRef.compareAndSet(entry, null)) {
			MartiniResult result = entry.getValue();
			publishScenarioEnd(result);

			Thread thread = entry.getKey();
			scope.clear(thread);
		}
	}

	@Override
	protected Sampler nextIsNull() throws NextIsNullException {
		endScenario();
		return super.nextIsNull();
	}

	protected void endScenario() {
		if (null != martiniResult) {
			publishScenarioEnd(martiniResult);
			martiniResult = null;
		}
	}

	protected void publishScenarioEnd(MartiniResult result) {
		MartiniScenarioEvent event = new AfterScenarioEvent(this, result);
		eventPublisher.publish(event);
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}

	@Override
	public void testEnded() {
		endScenario();

		synchronized (SUITE_ENDED) {
			if (!SUITE_ENDED.get()) {
				MartiniSuiteEvent event = new AfterSuiteEvent(this, suiteIdentifier);
				eventPublisher.publish(event);
				SUITE_ENDED.set(true);
			}
		}

		eventPublisher = null;
		suiteIdentifier = null;
		scope = null;
		setupContextRef = null;
	}
}