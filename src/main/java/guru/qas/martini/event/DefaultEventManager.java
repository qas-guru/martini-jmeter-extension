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

import org.apache.http.HttpEntity;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniConstants;
import guru.qas.martini.gherkin.Recipe;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultEventManager implements EventManager {

	protected final MartiniEventPublisher publisher;

	@Autowired
	DefaultEventManager(MartiniEventPublisher publisher) {
		this.publisher = publisher;
	}

	@Override
	public void publishBeforeSuite(Object source, JMeterContext threadContext) {
		JMeterVariables variables = threadContext.getVariables();
		long timestamp = Long.valueOf(variables.get("TESTSTART.MS"));
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier(threadContext);
		BeforeSuitePayload payload = new BeforeSuitePayload(timestamp, suiteIdentifier);
		BeforeSuiteEvent event = new BeforeSuiteEvent(source, payload);
		publisher.publish(event);
	}

	protected SuiteIdentifier getSuiteIdentifier(JMeterContext context) {
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
	public void publishAfterSuite(Object source, JMeterContext threadContext) {
		AfterSuitePayload payload = getAfterSuitePayload(threadContext);
		AfterSuiteEvent event = new AfterSuiteEvent(source, payload);
		publisher.publish(event);
	}

	protected AfterSuitePayload getAfterSuitePayload(JMeterContext threadContext) {
		long timestamp = System.currentTimeMillis();
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier(threadContext); // can set this on thread?
		Status status = getStatus(threadContext);
		Exception exception = getException(threadContext);
		Collection<HttpEntity> embedded = getEmbedded(threadContext);
		return new AfterSuitePayload(timestamp, suiteIdentifier, status, exception, embedded);
	}

	protected Status getStatus(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(MartiniConstants.VARIABLE_STATUS);
		return Status.class.isInstance(o) ? Status.class.cast(o) : null;
	}

	protected Exception getException(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(MartiniConstants.VARIABLE_EXCEPTION);
		return Exception.class.isInstance(o) ? Exception.class.cast(o) : null;
	}

	protected Collection<HttpEntity> getEmbedded(JMeterContext context) {
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(MartiniConstants.VARIABLE_EMBEDDED);
		return Collection.class.isInstance(o) ? Collection.class.cast(o) : null;
	}

	@Override
	public void publishBeforeScenario(Object source, JMeterContext threadContext, Martini martini) {
		BeforeScenarioPayload payload = getBeforeScenarioPayload(threadContext, martini);
		BeforeScenarioEvent event = new BeforeScenarioEvent(source, payload);
		publisher.publish(event);
	}

	protected BeforeScenarioPayload getBeforeScenarioPayload(JMeterContext threadContext, Martini martini) {
		long timestamp = System.currentTimeMillis();
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier(threadContext);
		JMeterThread thread = threadContext.getThread();
		String threadName = thread.getThreadName();
		Recipe recipe = martini.getRecipe();
		String recipeId = recipe.getId();
		ScenarioIdentifier scenarioIdentifier = new ScenarioIdentifier(suiteIdentifier, threadName, recipeId);
		return new BeforeScenarioPayload(timestamp, scenarioIdentifier, martini);
	}
}
