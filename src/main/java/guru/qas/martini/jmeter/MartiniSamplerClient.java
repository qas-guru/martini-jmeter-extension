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

package guru.qas.martini.jmeter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.convert.ConversionService;

import com.google.common.base.Throwables;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.Status;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.DefaultStepResult;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.step.UnimplementedStepException;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniSamplerClient extends AbstractJavaSamplerClient {

	private DefaultMartiniResult martiniResult;
	private EventManager eventManager;

	private void doSetup() {
		Martini martini = MartiniPreProcessor.getMartini();
		if (null != martini) {
			eventManager = SpringPreProcessor.getBean(EventManager.class);
			SuiteIdentifier suiteIdentifier = SpringPreProcessor.getBean(SuiteIdentifier.class);
			Categories categories = SpringPreProcessor.getBean(Categories.class);

			Set<String> categorizations = categories.getCategorizations(martini);

			JMeterContext jmeterContext = JMeterContextService.getContext();
			AbstractThreadGroup threadGroup = jmeterContext.getThreadGroup();
			JMeterThread thread = jmeterContext.getThread();

			martiniResult = DefaultMartiniResult.builder()
				.setMartini(martini)
				.setMartiniSuiteIdentifier(suiteIdentifier)
				.setCategorizations(categorizations)
				.setThreadGroupName(threadGroup.getName())
				.setThreadName(thread.getThreadName())
				.build();
			eventManager.publishBeforeScenario(this, martiniResult);
		}
	}

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		SampleResult sampleResult = new SampleResult();
		doSetup();
		try {
			if (null == martiniResult) {
				sampleResult.setSuccessful(false);
				sampleResult.setResponseMessage("no Martini available");
			}
			else {
				try {
					sampleResult.sampleStart();
					executeMartini();
					sampleResult.sampleEnd();

					if (Status.PASSED != martiniResult.getStatus()) {
						sampleResult.setSuccessful(false);
						List<StepResult> stepResults = martiniResult.getStepResults();

						Exception e = null;
						for (Iterator<StepResult> i = stepResults.iterator(); null == e && i.hasNext(); ) {
							StepResult stepResult = i.next();
							e = stepResult.getException();
						}
						if (null != e) {
							String message = Throwables.getStackTraceAsString(e);
							sampleResult.setResponseMessage(message);
						}
					}
					else {
						sampleResult.setSuccessful(true);
					}
				}
				catch (Exception e) {
					sampleResult.sampleEnd();
					sampleResult.setSuccessful(false);
					String stackTrace = Throwables.getStackTraceAsString(e);
					sampleResult.setResponseMessage("unable to execute Martini: " + stackTrace);
				}
			}
		}
		finally {
			doTeardown();
		}

		return sampleResult;
	}

	public void executeMartini() throws InvocationTargetException, IllegalAccessException {
		Martini martini = martiniResult.getMartini();
		try {
			Map<Step, StepImplementation> stepIndex = martini.getStepIndex();
			martiniResult.setStartTimestamp(System.currentTimeMillis());

			DefaultStepResult lastResult = null;
			for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {

				Step step = mapEntry.getKey();
				eventManager.publishBeforeStep(this, martiniResult);

				StepImplementation implementation = mapEntry.getValue();
				if (null == lastResult || Status.PASSED == lastResult.getStatus()) {
					lastResult = execute(step, implementation);
				}
				else {
					lastResult = new DefaultStepResult(step, implementation);
					lastResult.setStatus(Status.SKIPPED);
				}
				martiniResult.add(lastResult);
				eventManager.publishAfterStep(this, martiniResult);
			}
		}
		finally {
			martiniResult.setEndTimestamp(System.currentTimeMillis());
			List<StepResult> stepResults = martiniResult.getStepResults();

			Long executionTime = null;
			for (StepResult stepResult : stepResults) {
				Long elapsed = stepResult.getExecutionTime(TimeUnit.MILLISECONDS);
				if (null == executionTime) {
					executionTime = elapsed;
				}
				else if (null != elapsed) {
					executionTime += elapsed;
				}
			}
			martiniResult.setExecutionTimeMs(executionTime);
			eventManager.publishAfterScenario(this, martiniResult);
		}
	}

	protected DefaultStepResult execute(Step step, StepImplementation implementation)
		throws InvocationTargetException, IllegalAccessException {
		getNewLogger().info("executing @{} {}", step.getKeyword().trim(), step.getText().trim());

		DefaultStepResult result = new DefaultStepResult(step, implementation);
		result.setStartTimestamp(System.currentTimeMillis());
		try {
			assertImplemented(step, implementation);
			Object[] arguments = getArguments(step, implementation);
			Object bean = getBean(implementation);
			Object o = execute(implementation, bean, arguments);
			if (HttpEntity.class.isInstance(o)) {
				result.add(HttpEntity.class.cast(o));
			}
			result.setStatus(Status.PASSED);
		}
		catch (UnimplementedStepException e) {
			result.setException(e);
			result.setStatus(Status.SKIPPED);
		}
		catch (Exception e) {
			result.setException(e);
			result.setStatus(Status.FAILED);
		}
		finally {
			result.setEndTimestamp(System.currentTimeMillis());
		}
		return result;
	}

	protected void assertImplemented(Step step, StepImplementation implementation) throws UnimplementedStepException {
		Method method = implementation.getMethod();
		if (null == method) {
			throw UnimplementedStepException.builder().build(step);
		}
	}

	protected Object[] getArguments(Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		Parameter[] parameters = method.getParameters();
		Object[] arguments = new Object[parameters.length];

		if (parameters.length > 0) {

			String text = step.getText();
			Pattern pattern = implementation.getPattern();
			Matcher matcher = pattern.matcher(text);
			checkState(matcher.find(),
				"unable to locate substitution parameters for pattern %s with input %s", pattern.pattern(), text);

			int groupCount = matcher.groupCount();
			ConversionService conversionService = SpringPreProcessor.getBean(ConversionService.class);
			for (int i = 0; i < groupCount; i++) {
				String parameterAsString = matcher.group(i + 1);
				Parameter parameter = parameters[i];
				Class<?> parameterType = parameter.getType();

				Object converted = conversionService.convert(parameterAsString, parameterType);
				arguments[i] = converted;
			}
		}
		return arguments;
	}

	protected Object getBean(StepImplementation implementation) {
		Method method = implementation.getMethod();
		Class<?> declaringClass = method.getDeclaringClass();
		AutowireCapableBeanFactory beanFactory = SpringPreProcessor.getApplicationContext().getAutowireCapableBeanFactory();
		return beanFactory.getBean(declaringClass);
	}

	protected Object execute(
		StepImplementation implementation,
		Object bean,
		Object[] arguments
	) throws InvocationTargetException, IllegalAccessException {
		Method method = implementation.getMethod();
		return method.invoke(bean, arguments);
	}

	private void doTeardown() {
		if (null != martiniResult && null != eventManager) {
			eventManager.publishAfterScenario(this, martiniResult);
		}
		martiniResult = null;
		eventManager = null;
	}
}
