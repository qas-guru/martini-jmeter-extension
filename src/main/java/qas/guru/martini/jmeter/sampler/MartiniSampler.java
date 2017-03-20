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

package qas.guru.martini.jmeter.sampler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopTestNowException;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import gherkin.ast.Feature;
import gherkin.ast.Step;
import gherkin.pickles.Pickle;
import guru.qas.martini.Martini;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;
import qas.guru.martini.DefaultIconManager;
import qas.guru.martini.event.ScenarioEvent;

import static com.google.common.base.Preconditions.*;
import static qas.guru.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler {

	private static final long serialVersionUID = -5644094193554791266L;

	private final Logger logger;

	public MartiniSampler() {
		super();
		logger = LoggingManager.getLoggerFor(getClass().getName());
		DefaultIconManager iconManager = DefaultIconManager.getInstance();
		iconManager.registerIcons(getClass());
	}

	@Override
	public SampleResult sample(Entry entry) {
		Martini martini = getMartiniUnchecked();
		publishStarting(martini);

		SampleResult sampleResult = null;
		try {
			martini = getMartiniChecked();
			sampleResult = sample(martini);
		}
		catch (Exception e) {
			logger.error("unable to execute Martini sample", e);
			sampleResult = getError(martini, e);
		}
		finally {
			publishEnded(sampleResult, martini);
		}
		return sampleResult;
	}

	protected void publishStarting(Martini martini) {
		ApplicationEventPublisher publisher = getApplicationContext();
		JMeterContext threadContext = getThreadContext();
		ScenarioEvent starting = ScenarioEvent.getStarting(threadContext, martini);
		publisher.publishEvent(starting);
	}

	protected ApplicationContext getApplicationContext() {
		JMeterVariables variables = getVariables();
		Object o = variables.getObject(VARIABLE_APPLICATION_CONTEXT);
		if (!ApplicationContext.class.isInstance(o)) {
			String message = String.format(
				"property %s is not an instance of ApplicationContext: %s", VARIABLE_APPLICATION_CONTEXT, o);
			throw new JMeterStopTestNowException(message);
		}
		return ApplicationContext.class.cast(o);
	}

	protected JMeterVariables getVariables() {
		JMeterContext threadContext = super.getThreadContext();
		return threadContext.getVariables();
	}

	protected SampleResult getError(Martini martini, Exception e) {
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(null == martini ? "UNKNOWN" : getLabel(martini));
		sampleResult.setSuccessful(false);
		AssertionResult assertResult = new AssertionResult("Martini Execution Error");
		assertResult.setError(true);
		assertResult.setFailure(true);
		assertResult.setFailureMessage(e.getMessage());
		sampleResult.addAssertionResult(assertResult);
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
				getSubResult(step, implementation) : getSkipped(step);
			sampleResult.addSubResult(subResult);
			sampleResult.setSuccessful(sampleResult.isSuccessful() && subResult.isSuccessful());
		}
		return sampleResult;
	}

	/**
	 * Returns Martini stored in variable, or null. This allows listeners to inject changes prior to the
	 * Sampler continuing execution.
	 *
	 * @return current Martini from JMeter variables
	 */
	protected Martini getMartiniUnchecked() {
		Object o = getMartiniObject();
		return Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
	}

	protected Object getMartiniObject() {
		JMeterVariables variables = getVariables();
		return variables.getObject(VARIABLE_MARTINI);
	}

	protected Martini getMartiniChecked() {
		Object o = getMartiniObject();
		checkState(Martini.class.isInstance(o), "variable %s is not an instance of Martini: %s", VARIABLE_MARTINI, o);
		return Martini.class.cast(o);
	}

	protected SampleResult getSubResult(Step step, StepImplementation implementation) {
		String text = step.getText();

		SampleResult result = new SampleResult();
		result.setSuccessful(true);
		String label = step.getText();
		result.sampleStart();

		try {
			ApplicationContext applicationContext = this.getApplicationContext();
			ConversionService conversionService = getConversionService(applicationContext);

			Method method = implementation.getMethod();
			Matcher matcher = implementation.getPattern().matcher(text);
			MatchResult matchResult = matcher.toMatchResult();

			Parameter[] parameters = method.getParameters();
			Object[] arguments = new Object[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				String parameterAsString = matchResult.group(i + 1);
				Parameter parameter = parameters[i];
				Class<?> parameterType = parameter.getType();
				Object converted = conversionService.convert(parameterAsString, parameterType);
				arguments[i] = converted;
			}

			Class<?> declaringClass = implementation.getMethod().getDeclaringClass();
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
		}
		return result;
	}

	// TODO: Should be registered in Martini as default.
	protected ConversionService getConversionService(ApplicationContext applicationContext) {
		ConversionService conversionService;
		try {
			conversionService = applicationContext.getBean(ConversionService.class);
		}
		catch (Exception e) {
			conversionService = new DefaultConversionService();
		}
		return conversionService;
	}

	protected SampleResult getSkipped(Step step) {
		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String label = String.format("SKIPPED: %s", step.getText());
		result.setSampleLabel(label);
		return result;
	}

	protected void publishEnded(SampleResult result, Martini martini) {
		ApplicationEventPublisher publisher = getApplicationContext();
		JMeterContext threadContext = getThreadContext();
		ScenarioEvent starting = ScenarioEvent.getEnded(threadContext, martini, result);
		publisher.publishEvent(starting);
	}
}
