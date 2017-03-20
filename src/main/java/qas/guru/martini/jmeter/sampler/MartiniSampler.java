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

import java.util.List;

import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopTestNowException;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import gherkin.ast.Feature;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleStep;
import guru.qas.martini.Martini;
import guru.qas.martini.gherkin.Recipe;
import qas.guru.martini.event.ScenarioEvent;

import static com.google.common.base.Preconditions.*;
import static qas.guru.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler implements TestBean {

	// TODO: own GUI/icon?

	private static final long serialVersionUID = -5644094193554791266L;
	protected static final String GUI = "org.apache.jmeter.config.gui.SimpleConfigGui";

	private final Logger logger;

	public MartiniSampler() {
		super();
		logger = LoggingManager.getLoggerFor(getClass().getName());
	}

	@Override
	public boolean applies(ConfigTestElement configElement) {
		JMeterProperty property = configElement.getProperty(TestElement.GUI_CLASS);
		String guiClass = property.getStringValue();
		return GUI.equals(guiClass);
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
		Recipe recipe = martini.getRecipe();
		Pickle pickle = recipe.getPickle();

		String label = getLabel(martini);
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(label);
		sampleResult.setSuccessful(true);
		sampleResult.sampleStart();

		List<PickleStep> steps = pickle.getSteps();
		for (PickleStep step : steps) {
			SampleResult subResult;
			if (sampleResult.isSuccessful()) {
				subResult = getSubResult(step);
			}
			else {
				subResult = getSkipped(step);
			}
			sampleResult.addSubResult(subResult);
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

	protected SampleResult getSubResult(PickleStep step) {
		SampleResult result = new SampleResult();
		result.setSuccessful(true);
		String label = step.getText();
		result.sampleStart();

		try {
			// do something here
		}
		catch (Exception e) {
			result.setSuccessful(false);
			label = "FAIL: " + label;
		}
		result.sampleEnd();
		result.setSampleLabel(label);
		return result;
	}

	protected SampleResult getSkipped(PickleStep step) {
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
