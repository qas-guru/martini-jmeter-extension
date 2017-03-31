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

package guru.qas.martini.event;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.gherkin.Recipe;

import static guru.qas.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEventManager implements EventManager {

	protected final MartiniEventPublisher publisher;

	@Autowired
	DefaultEventManager(MartiniEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void publishBeforeSuite(Object source, JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		long timestamp = Long.valueOf(variables.get("TESTSTART.MS"));
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
		BeforeSuitePayload payload = new BeforeSuitePayload(timestamp, suiteIdentifier);
		BeforeSuiteEvent event = new BeforeSuiteEvent(source, payload);
		publisher.publish(event);
	}

	protected SuiteIdentifier getSuiteIdentifier() {
		String hostname = JMeterUtils.getLocalHostName();
		String suite = "";
		ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
		String threadGroupName = threadGroup.getName();

		return SuiteIdentifier.builder()
			.setHostName(hostname)
			.setSuiteName(suite)
			.setThreadGroupName(threadGroupName)
			.build();
	}

	@Override
	public void publishAfterSuite(Object source, JMeterContext context) {
		AfterSuitePayload payload = getAfterSuitePayload(context);
		AfterSuiteEvent event = new AfterSuiteEvent(source, payload);
		publisher.publish(event);
	}

	protected AfterSuitePayload getAfterSuitePayload(JMeterContext threadContext) {
		long timestamp = System.currentTimeMillis();
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
		Status status = getStatus(threadContext);
		Exception exception = getException(threadContext);
		Collection<HttpEntity> embedded = getEmbedded(threadContext);
		return new AfterSuitePayload(timestamp, suiteIdentifier, status, exception, embedded);
	}

	protected Status getStatus(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(VARIABLE_STATUS);
		return Status.class.isInstance(o) ? Status.class.cast(o) : null;
	}

	protected Exception getException(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(VARIABLE_EXCEPTION);
		return Exception.class.isInstance(o) ? Exception.class.cast(o) : null;
	}

	protected Collection<HttpEntity> getEmbedded(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(VARIABLE_EMBEDDED);
		return Collection.class.isInstance(o) ? Collection.class.cast(o) : null;
	}

	@Override
	public void publishBeforeScenario(Object source, JMeterContext context, Martini martini) {
		BeforeScenarioPayload payload = getBeforeScenarioPayload(context, martini);
		BeforeScenarioEvent event = new BeforeScenarioEvent(source, payload);
		publisher.publish(event);
	}

	protected BeforeScenarioPayload getBeforeScenarioPayload(JMeterContext threadContext, Martini martini) {
		long timestamp = System.currentTimeMillis();
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
		JMeterThread thread = threadContext.getThread();
		String threadName = thread.getThreadName();
		Recipe recipe = martini.getRecipe();
		String recipeId = recipe.getId();
		ScenarioIdentifier scenarioIdentifier = new ScenarioIdentifier(suiteIdentifier, threadName, recipeId);
		return new BeforeScenarioPayload(timestamp, scenarioIdentifier, martini);
	}

	@Override
	public void publishAfterScenario(Object source, JMeterContext context, Martini martini, SampleResult result) {
		AfterScenarioPayload payload = getAfterScenarioPayload(context, martini, result);
		AfterScenarioEvent event = new AfterScenarioEvent(source, payload);
		publisher.publish(event);
	}

	protected AfterScenarioPayload getAfterScenarioPayload(
		JMeterContext context,
		Martini martini,
		SampleResult result
	) {
		long timestamp = result.getEndTime();
		ScenarioIdentifier scenarioIdentifier = getScenarioIdentifier(martini, context);
		Status status = getSamplerObject(context, VARIABLE_STATUS, Status.class);
		Exception exception = getSamplerObject(context, VARIABLE_EXCEPTION, Exception.class);
		@SuppressWarnings("unchecked")
		List<HttpEntity> embedded = getSamplerObject(context, VARIABLE_EMBEDDED, List.class);
		return new AfterScenarioPayload(timestamp, scenarioIdentifier, martini, status, exception, embedded);
	}

	protected ScenarioIdentifier getScenarioIdentifier(Martini martini, JMeterContext context) {
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
		JMeterThread thread = context.getThread();
		String threadName = thread.getThreadName();
		Recipe recipe = martini.getRecipe();
		String recipeId = recipe.getId();
		return new ScenarioIdentifier(suiteIdentifier, threadName, recipeId);
	}

	protected <T> T getSamplerObject(JMeterContext context, String property, Class<T> implementation) {
		Map<String, Object> samplerContext = context.getSamplerContext();
		Object status = samplerContext.get(property);
		return implementation.isInstance(status) ? implementation.cast(status) : null;
	}

	@Override
	public void publishBeforeStep(Object source, JMeterContext context, Martini martini, Step step) {
		BeforeStepPayload payload = getBeforeStepPayload(context, martini, step);
		BeforeStepEvent event = new BeforeStepEvent(source, payload);
		publisher.publish(event);
	}

	protected BeforeStepPayload getBeforeStepPayload(JMeterContext context, Martini martini, Step step) {
		long timestamp = System.currentTimeMillis();
		ScenarioIdentifier scenarioIdentifier = this.getScenarioIdentifier(martini, context);
		return new BeforeStepPayload(timestamp, scenarioIdentifier, martini, step);
	}

	@Override
	public void publishAfterStep(
		Object source, JMeterContext context, Martini martini, Step step, SampleResult result
	) {
		AfterStepPayload payload = getAfterStepPayload(result, martini, context, step);
		AfterStepEvent event = new AfterStepEvent(source, payload);
		publisher.publish(event);
	}

	protected AfterStepPayload getAfterStepPayload(
		SampleResult result,
		Martini martini,
		JMeterContext context,
		Step step
	) {
		long timestamp = result.getEndTime();
		ScenarioIdentifier scenarioIdentifier = getScenarioIdentifier(martini, context);
		Status status = getSamplerObject(context, VARIABLE_STATUS, Status.class);
		Exception exception = getSamplerObject(context, VARIABLE_EXCEPTION, Exception.class);
		@SuppressWarnings("unchecked")
		List<HttpEntity> embedded = getSamplerObject(context, VARIABLE_EMBEDDED, List.class);
		return new AfterStepPayload(timestamp, scenarioIdentifier, martini, step, status, exception, embedded);
	}
}

