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

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.jmeter.SpringBeanUtil;
import guru.qas.martini.jmeter.result.JMeterMartini;
import guru.qas.martini.jmeter.result.JMeterMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.scope.MartiniScenarioScope;

@SuppressWarnings({"WeakerAccess", "unused"})
@Deprecated
public class MartiniScenarioController extends GenericController implements TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 9021149216602507240L;

	public static final String KEY = JMeterMartiniResult.class.getName();

	protected transient Logger logger;
	protected transient EventManager eventManager;
	protected transient MartiniScenarioScope scenarioScope;

	protected transient MartiniResult martiniResult;

	public MartiniScenarioController() {
		super();
		init();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	private void init() {
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public Object clone() {
		MartiniScenarioController clone = MartiniScenarioController.class.cast(super.clone());
		clone.logger = logger;
		clone.eventManager = eventManager;
		clone.scenarioScope = scenarioScope;
		return clone;
	}

	@Override
	public void testStarted() {
		setUpEventManager();
		setUpScenarioScope();
	}

	protected void setUpEventManager() {
		eventManager = SpringBeanUtil.getBean(null, EventManager.class.getName(), EventManager.class);
	}

	protected void setUpScenarioScope() {
		ApplicationContext applicationContext = SpringBeanUtil.getApplicationContext();
		scenarioScope = applicationContext.getBean(MartiniScenarioScope.class);
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		martiniResult = null;
	}

	@Override
	public Sampler next() {
		if (null == martiniResult) {
			startScenario();
		}
		Sampler sampler = super.next();
		if (null != sampler) {
			JMeterContext threadContext = sampler.getThreadContext();
			Map<String, Object> samplerContext = threadContext.getSamplerContext();
			samplerContext.put(KEY, martiniResult);
		}
		return sampler;
	}

	protected void startScenario() {
		endScenario();
		JMeterMartini martini = JMeterMartini.builder()
			.setFeatureName(JMeterContextService.getContext().getThreadGroup().getName())
			.setScenarioName(super.getName())
			.build();
		martiniResult = JMeterMartiniResult.builder().setJMeterMartini(martini).build();
		eventManager.publishBeforeScenario(this, martiniResult);
	}

	@Override
	protected Sampler nextIsNull() throws NextIsNullException {
		endScenario();
		return super.nextIsNull();
	}

	protected void endScenario() {
		if (null != martiniResult && null != eventManager) {
			eventManager.publishAfterScenario(this, martiniResult);
		}
		martiniResult = null;
	}

	@Override
	public void testEnded() {
		endScenario();
		if (null != scenarioScope) {
			scenarioScope.clear();
			scenarioScope = null;
		}
		eventManager = null;
		martiniResult = null;
		logger = null;
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}
