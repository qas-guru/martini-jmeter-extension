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

package guru.qas.martini.jmeter.sampler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;

import gherkin.ast.Feature;
import gherkin.ast.Step;
import gherkin.pickles.Pickle;
import guru.qas.martini.Martini;
import guru.qas.martini.event.EventManager;
import guru.qas.martini.event.Status;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;

import static com.google.common.base.Preconditions.checkState;
import static guru.qas.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler {

	private static final long serialVersionUID = -5644094193554791266L;

	protected Logger logger;

	public MartiniSampler() {
		super();
		init();
	}

	protected void init() {
		Class<? extends MartiniSampler> implementation = getClass();
		String category = implementation.getName();
		logger = LoggingManager.getLoggerFor(category);
	}

	@Override
	public SampleResult sample(Entry entry) {
		Martini martini = getMartini();
		publishBeforeScenario(martini);

		SampleResult sampleResult;
		try {
			sampleResult = sample(martini);
		}
		catch (Exception e) {
			logger.error("unable to execute Martini sample", e);
			sampleResult = getError(martini, e);
		}

		publishAfterScenario(martini, sampleResult);
		return sampleResult;
	}

	protected void publishBeforeScenario(Martini martini) {
		EventManager eventManager = getEventManager();
		JMeterContext threadContext = getThreadContext();
		eventManager.publishBeforeScenario(this, threadContext, martini);
	}

	protected EventManager getEventManager() {
		ApplicationContext applicationContext = this.getApplicationContext();
		return applicationContext.getBean(EventManager.class);
	}

	protected void publishAfterScenario(Martini martini, SampleResult sampleResult) {
		EventManager eventManager = getEventManager();
		JMeterContext threadContext = getThreadContext();
		eventManager.publishAfterScenario(this, threadContext, martini, sampleResult);
	}

	protected ApplicationContext getApplicationContext() {
		JMeterContext threadContext = super.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		JMeterProperty property = threadGroup.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = null == property ? null : property.getObjectValue();
		if (!ApplicationContext.class.isInstance(o)) {
			throw new JMeterStopThreadException("unable to retrieve Spring ApplicationContext from ThreadGroup");
		}
		return ApplicationContext.class.cast(o);
	}

	protected SampleResult getError(Martini martini, Exception e) {
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(null == martini ? "UNKNOWN" : getLabel(martini));
		sampleResult.setSuccessful(false);

		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		samplerContext.put("status", Status.FAILED);
		samplerContext.put("exception", e);

		return sampleResult;
	}

	protected String getLabel(Martini martini) {
		Recipe recipe = martini.getRecipe();

		Feature feature = recipe.getFeature();
		String featureName = feature.getName();

		Pickle pickle = recipe.getPickle();
		String pickleName = pickle.getName();
		return String.format("%s (%s)", pickleName, featureName);
	}

	protected SampleResult sample(Martini martini) {
		Map<Step, StepImplementation> stepIndex = martini.getStepIndex();

		String label = getLabel(martini);
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(label);
		sampleResult.setSuccessful(true);
		sampleResult.sampleStart();

		for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {
			Step step = mapEntry.getKey();
			StepImplementation implementation = mapEntry.getValue();

			SampleResult subResult = sampleResult.isSuccessful() ?
				getSubResult(martini, step, implementation) : getSkipped(step);
			sampleResult.addSubResult(subResult);
			sampleResult.setSuccessful(sampleResult.isSuccessful() && subResult.isSuccessful());
		}

		return sampleResult;
	}

	/**
	 * Returns Martini stored in variable, or null. This allows listeners to inject changes prior to the
	 * Sampler continuing execution.
	 *
	 * @return currentRef Martini from JMeter variables
	 */
	protected Martini getMartini() {
		JMeterContext threadContext = getThreadContext();
		JMeterThread thread = threadContext.getThread();
		String threadName = thread.getThreadName();
		String key = String.format("martini.%s", threadName);

		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(key);

		if (!Martini.class.isInstance(o)) {
			throw new JMeterStopThreadException("unable to retrieve Martini from JMeterVariables");
		}
		return Martini.class.cast(o);
	}

	protected SampleResult getSubResult(Martini martini, Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		return null == method ? getUnimplementedSubResult(step) : getSubResult(martini, step, method, implementation.getPattern());
	}

	protected SampleResult getSubResult(Martini martini, Step step, Method method, Pattern pattern) {
		publishBeforeStep(martini, step);

		String label = getLabel(step);
		SampleResult result = new SampleResult();
		result.setSuccessful(true);
		result.sampleStart();

		try {

			ApplicationContext applicationContext = this.getApplicationContext();
			Parameter[] parameters = method.getParameters();
			Object[] arguments = new Object[parameters.length];

			if (parameters.length > 0) {
				String text = step.getText();
				Matcher matcher = pattern.matcher(text);
				checkState(matcher.find(),
					"unable to locate substitution parameters for pattern %s with input %s", pattern.pattern(), text);

				ConversionService conversionService = applicationContext.getBean(ConversionService.class);

				int groupCount = matcher.groupCount();
				for (int i = 0; i < groupCount; i++) {
					String parameterAsString = matcher.group(i + 1);
					Parameter parameter = parameters[i];
					Class<?> parameterType = parameter.getType();
					Object converted = conversionService.convert(parameterAsString, parameterType);
					arguments[i] = converted;
				}
			}

			Class<?> declaringClass = method.getDeclaringClass();
			Object bean = applicationContext.getBean(declaringClass);
			method.invoke(bean, arguments);

		}
		catch (Exception e) {
			result.setSuccessful(false);
			label = "FAIL: " + label;
		}
		finally {
			result.sampleEnd();
			result.setSampleLabel(label);
			publishAfterStep(martini, step, result);
		}
		return result;
	}

	protected SampleResult getUnimplementedSubResult(Step step) {
		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String label = getLabel(step);
		result.setSampleLabel("UNIMPLEMENTED: " + label);
		return result;
	}

	protected String getLabel(Step step) {
		String keyword = step.getKeyword();
		String normalizedKeyword = null == keyword ? "" : keyword.trim();
		String text = step.getText();
		return normalizedKeyword.isEmpty() ? text : String.format("%s %s", normalizedKeyword, text);
	}

	protected void publishBeforeStep(Martini martini, Step step) {
		EventManager eventManager = getEventManager();
		JMeterContext threadContext = getThreadContext();
		eventManager.publishBeforeStep(this, threadContext, martini, step);
	}

	protected void publishAfterStep(Martini martini, Step step, SampleResult result) {
		EventManager eventManager = getEventManager();
		JMeterContext threadContext = getThreadContext();
		eventManager.publishAfterStep(this, threadContext, martini, step, result);
	}

	protected SampleResult getSkipped(Step step) {
		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String keyword = step.getKeyword();
		String trimmedKeyword = null == keyword ? "" : keyword.trim();
		String text = step.getText();
		String label = String.format("SKIPPED: %s %s", trimmedKeyword, text);
		result.setSampleLabel(label);
		return result;
	}
}
