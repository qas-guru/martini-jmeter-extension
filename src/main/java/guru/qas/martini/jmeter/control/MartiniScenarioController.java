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

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor;
import guru.qas.martini.jmeter.result.JMeterMartini;
import guru.qas.martini.jmeter.result.JMeterMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniScenarioController extends GenericController implements TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 4383114502659399221L;
	protected static final String KEY = JMeterMartiniResult.class.getName();

	protected transient Logger logger;
	protected transient AtomicReference<EventManager> eventManagerRef;
	protected transient MartiniResult martiniResult;

	public MartiniScenarioController() {
		super();
		eventManagerRef = new AtomicReference<>();
	}

	@Override
	public Object clone() {
		MartiniScenarioController clone = MartiniScenarioController.class.cast(super.clone());
		clone.eventManagerRef = eventManagerRef;
		return clone;
	}

	@Override
	public void testStarted() {
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		//startScenario(); // TODO: NOPE this gets called at once for ALL controllers
		martiniResult = null;
	}

	protected void startScenario() {
		endScenario();
		JMeterMartini martini = JMeterMartini.builder()
			.setFeatureName(JMeterContextService.getContext().getThreadGroup().getName())
			.setScenarioName(super.getName())
			.build();
		martiniResult = JMeterMartiniResult.builder().setJMeterMartini(martini).build();
		EventManager eventManager = getEventManager();
		eventManager.publishBeforeScenario(this, martiniResult);
	}

	protected EventManager getEventManager() {
		EventManager eventManager = eventManagerRef.get();
		if (null == eventManager) {
			ApplicationContext context = MartiniSpringPreProcessor.getApplicationContext();
			EventManager instance = context.getBean(EventManager.class);
			eventManager = eventManagerRef.compareAndSet(null, instance) ? instance : eventManagerRef.get();
		}
		return eventManager;
	}

	@Override
	public Sampler next() {
		if (null == martiniResult) {
			startScenario();
		}
		return super.next();
	}

	@Override
	protected Sampler nextIsASampler(Sampler element) throws NextIsNullException {
		Sampler sampler = super.nextIsASampler(element);
		if (null != sampler) {
			JMeterContext threadContext = sampler.getThreadContext();
			Map<String, Object> samplerContext = threadContext.getSamplerContext();
			samplerContext.put(KEY, martiniResult);
		}
		return sampler;
	}

	@Override
	protected Sampler nextIsNull() throws NextIsNullException {
		endScenario();
		return super.nextIsNull();
	}

	protected void endScenario() {
		if (null != martiniResult) {
			EventManager eventManager = getEventManager();
			eventManager.publishAfterScenario(this, martiniResult);
			martiniResult = null;
		}
	}

	@Override
	public void testEnded() {
		endScenario();
		eventManagerRef.set(null);
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}
