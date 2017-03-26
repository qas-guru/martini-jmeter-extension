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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.ConfigTestElement;
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
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;

import qas.guru.martini.event.DefaultAfterScenarioEvent;
import qas.guru.martini.event.DefaultAfterStepEvent;
import qas.guru.martini.event.DefaultBeforeScenarioEvent;
import qas.guru.martini.event.DefaultBeforeStepEvent;

import static com.google.common.base.Preconditions.checkState;
import static qas.guru.martini.MartiniConstants.*;

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
	public boolean applies(ConfigTestElement configElement) {
		return super.applies(configElement);
	}

	@Override
	public SampleResult sample(Entry entry) {
		Martini martini = getMartini();
		publishBeforeEvent(martini);

		SampleResult sampleResult;
		try {
			sampleResult = sample(martini);
		}
		catch (Exception e) {
			logger.error("unable to execute Martini sample", e);
			sampleResult = getError(martini, e);
		}

		publishAfterEvent(martini, sampleResult);
		return sampleResult;
	}

	protected void publishBeforeEvent(Martini martini) {
		ApplicationContext applicationContext = getApplicationContext();
		JMeterContext threadContext = super.getThreadContext();
		DefaultBeforeScenarioEvent event = new DefaultBeforeScenarioEvent(martini, threadContext);
		applicationContext.publishEvent(event);
	}

	protected void publishAfterEvent(Martini martini, SampleResult sampleResult) {
		JMeterContext threadContext = super.getThreadContext();
		DefaultAfterScenarioEvent event = new DefaultAfterScenarioEvent(martini, threadContext, sampleResult);
		ApplicationContext applicationContext = getApplicationContext();
		applicationContext.publishEvent(event);
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
		publishBeforeStep(martini, step);

		String text = step.getText();

		SampleResult result = new SampleResult();
		result.setSuccessful(true);
		String label = String.format("%s %s", implementation.getKeyword(), step.getText());
		result.sampleStart();

		try {
			Method method = implementation.getMethod();

			ApplicationContext applicationContext = this.getApplicationContext();
			Parameter[] parameters = method.getParameters();
			Object[] arguments = new Object[parameters.length];

			if (parameters.length > 0) {
				Pattern pattern = implementation.getPattern();
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
			publishAfterStep(martini, step, result);
		}
		return result;
	}

	protected void publishBeforeStep(Martini martini, Step step) {
		JMeterContext context = super.getThreadContext();
		DefaultBeforeStepEvent event = new DefaultBeforeStepEvent(martini, step, context);
		ApplicationContext applicationContext = this.getApplicationContext();
		applicationContext.publishEvent(event);
	}

	protected void publishAfterStep(Martini martini, Step step, SampleResult result) {
		JMeterContext context = super.getThreadContext();
		DefaultAfterStepEvent event = new DefaultAfterStepEvent(martini, step, context, result);
		ApplicationContext applicationContext = this.getApplicationContext();
		applicationContext.publishEvent(event);
	}

	protected SampleResult getSkipped(Step step) {
		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String label = String.format("SKIPPED: %s", step.getText());
		result.setSampleLabel(label);
		return result;
	}
}
