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

import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;

import gherkin.ast.Feature;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleStep;
import guru.qas.martini.Martini;
import guru.qas.martini.gherkin.Recipe;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler implements TestBean {

	private static final long serialVersionUID = -5644094193554791266L;
	protected static final String GUI = "org.apache.jmeter.config.gui.SimpleConfigGui";

	@Override
	public SampleResult sample(Entry e) {
		Martini martini = getMartini();
		System.out.println("GOT MARTINI " + martini);
		return null == martini ? getFailure() : sample(martini);
	}

	protected SampleResult getFailure() {
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel("ERROR: unable to load Martini");
		sampleResult.setSuccessful(false);
		sampleResult.setStopTestNow(true);
		return sampleResult;
	}

	protected SampleResult sample(Martini martini) {
		Recipe recipe = martini.getRecipe();
		Feature feature = recipe.getFeature();
		String featureName = feature.getName();

		Pickle pickle = recipe.getPickle();
		String pickleName = pickle.getName();

		//TODO: temporary String label = String.format("%s (%s)", pickleName, featureName);
		String label = martini.toString();
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

	protected Martini getMartini() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject("martini");
		return Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
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

	@Override
	public boolean applies(ConfigTestElement configElement) {
		JMeterProperty property = configElement.getProperty(TestElement.GUI_CLASS);
		String guiClass = property.getStringValue();
		return GUI.equals(guiClass);
	}
}
