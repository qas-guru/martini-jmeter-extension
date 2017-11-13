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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.jmeter.config.Arguments;
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
import guru.qas.martini.runtime.event.EventManager;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.step.UnimplementedStepException;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniSamplerClient extends AbstractJavaSamplerClient {

	protected static final String PARAMETER = "generate.json.response";
	protected static final String PARAMETER_DEFAULT = Boolean.FALSE.toString();

	protected transient boolean generatingJson;
	protected transient EventManager eventManager;
	protected transient SuiteIdentifier suiteIdentifier;
	protected transient String threadGroupName;
	protected transient String threadName;
	protected transient DefaultMartiniResult martiniResult;

	@Override
	public Arguments getDefaultParameters() {
		Arguments defaults = new Arguments();
		defaults.addArgument(PARAMETER, PARAMETER_DEFAULT, null, "true to include Martini JSON results in response");
		return defaults;
	}

	/**
	 * This is executed once per instantiation of this class. Nothing specific to an
	 * individual test run should appear in this method.
	 *
	 * @param context ignored
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		eventManager = SpringPreProcessor.getBean(EventManager.class);
		suiteIdentifier = SpringPreProcessor.getBean(SuiteIdentifier.class);

		JMeterContext jmeterContext = JMeterContextService.getContext();
		AbstractThreadGroup threadGroup = jmeterContext.getThreadGroup();
		threadGroupName = threadGroup.getName();

		JMeterThread thread = jmeterContext.getThread();
		threadName = thread.getThreadName();

		String parameter = context.getParameter(PARAMETER, PARAMETER_DEFAULT).trim();
		generatingJson = Boolean.valueOf(parameter);
	}

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		SampleResult sampleResult = new SampleResult();
		long sampleStart = 0;
		long sampleEnd = 0;

		try {
			setup();
			setSampleLabel(sampleResult);
			execute();
			assertSuccess();
			sampleResult.setSuccessful(true);
			sampleStart = martiniResult.getStartTimestamp();
			sampleEnd = martiniResult.getEndTimestamp();
			setResponse();
		}
		catch (Exception e) {
			super.getNewLogger().warn("unable to execute Martini", e);
			sampleResult.setSuccessful(false);
			String message = Throwables.getStackTraceAsString(e);
			sampleResult.setResponseMessage(message);
		}
		finally {
			sampleResult.setStampAndTime(sampleStart, sampleEnd);
			teardown();
		}
		return sampleResult;
	}

	protected void setup() {
		Martini martini = MartiniPreProcessor.getMartini();
		checkNotNull(martini, "no Martini available");
		setup(martini);
	}

	protected void setSampleLabel(SampleResult sample) {
		Martini martini = martiniResult.getMartini();
		String id = martini.getId();
		sample.setSampleLabel(id);
	}

	protected void setup(Martini martini) {
		martiniResult = getMartiniResult(martini);
		eventManager.publishBeforeScenario(this, martiniResult);
	}

	protected DefaultMartiniResult getMartiniResult(Martini martini) {
		DefaultMartiniResult.Builder builder = DefaultMartiniResult.builder()
			.setMartini(martini)
			.setMartiniSuiteIdentifier(suiteIdentifier)
			.setThreadGroupName(threadGroupName)
			.setThreadName(threadName);
		setCategorizations(martini, builder);
		return builder.build();
	}

	protected void setCategorizations(Martini martini, DefaultMartiniResult.Builder builder) {
		Categories categories = SpringPreProcessor.getBean(Categories.class);
		Set<String> categorizations = categories.getCategorizations(martini);
		builder.setCategorizations(categorizations);
	}

	public void execute() throws InvocationTargetException, IllegalAccessException {
		try {
			Martini martini = martiniResult.getMartini();
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

	protected void assertSuccess() {
		Status status = martiniResult.getStatus();
		Exception e = Status.PASSED != status ? martiniResult.getException() : null;
		String message = null == e ? null : Throwables.getStackTraceAsString(e);
		checkState(null == e, "scenario failed: %s", message);
	}

	protected void setResponse() {
		if (this.generatingJson) {
			try {
				super.getNewLogger().info("would generate JSON here");
			}
			catch (Exception e) {
				super.getNewLogger().warn("unable to generate JSON", e);
			}
		}
	}

	private void teardown() {
		try {
			if (null != martiniResult && null != eventManager) {
				eventManager.publishAfterScenario(this, martiniResult);
			}
		}
		catch (Exception e) {
			super.getNewLogger().warn("unable to perform Martini teardown", e);
		}
		finally {
			martiniResult = null;
		}
	}

	/**
	 * Executes once during test lifetime, per thread. Nothing specific to an individual
	 * Martini run should be in this method.
	 *
	 * @param context ignored
	 */
	@Override
	public void teardownTest(JavaSamplerContext context) {
		eventManager = null;
		super.teardownTest(context);
	}
}
