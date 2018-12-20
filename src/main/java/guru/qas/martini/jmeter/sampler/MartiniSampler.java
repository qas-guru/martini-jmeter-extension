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

package guru.qas.martini.jmeter.sampler;

import java.io.Serializable;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.Status;
import guru.qas.martini.jmeter.SamplerContext;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.runtime.harness.MartiniCallable;

@SuppressWarnings({"RedundantThrows", "WeakerAccess"})
@Configurable
public class MartiniSampler extends AbstractGenericSampler
	implements Serializable, Cloneable, TestBean {

	private static final long serialVersionUID = -4970886293526746276L;

	public MartiniSampler() {
		super();
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() throws Exception {
		return new MartiniSamplerBeanInfoSupport();
	}

	@Override
	protected void completeSetup() throws Exception {
	}

	@Override
	protected void completeSample(SampleResult result) throws Exception {
		Martini martini = SamplerContext.getMartini();
		setLabel(result, martini);

		MartiniResult martiniResult = getMartiniResult(martini);
		setSubResults(result, martiniResult);
		setSuccessful(result, martiniResult);
	}

	protected void setLabel(SampleResult result, Martini martini) {
		String scenarioName = martini.getScenarioName();
		result.setSampleLabel(scenarioName);
	}

	protected MartiniResult getMartiniResult(Martini martini) throws Exception {
		Callable<MartiniResult> callable = getCallable(martini);
		return callable.call();
	}

	protected Callable<MartiniResult> getCallable(Martini martini) {
		ConfigurableApplicationContext springContext = SamplerContext.getSpringApplicationContext();
		MartiniCallable callable = new MartiniCallable(martini);
		AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
		beanFactory.autowireBean(callable);
		beanFactory.initializeBean(callable, callable.getClass().getName());
		return callable;
	}

	protected void setSubResults(SampleResult parent, MartiniResult martiniResult) {
		List<StepResult> stepResults = martiniResult.getStepResults();
		stepResults.stream().map(this::getSubResult).forEach(parent::addSubResult);
	}

	protected SampleResult getSubResult(StepResult stepResult) {
		SampleResult subResult = new SampleResult();
		setLabel(subResult, stepResult);
		setExecutionTime(subResult, stepResult);
		setSuccessful(subResult, stepResult);
		return subResult;
	}

	protected void setLabel(SampleResult subResult, StepResult stepResult) {
		Step step = stepResult.getStep();
		String keyword = step.getKeyword();
		String text = step.getText();
		String label = String.format("%s %s", keyword, text);
		subResult.setSampleLabel(label);
	}

	protected void setExecutionTime(SampleResult subResult, StepResult stepResult) {
		stepResult.getExecutionTime(TimeUnit.MILLISECONDS).ifPresent(elapsed -> {
			if (subResult.isStampedAtStart()) {
				stepResult.getStartTimestamp()
					.ifPresent(timestamp -> subResult.setStampAndTime(timestamp, elapsed));
			}
			else {
				stepResult.getEndTimestamp()
					.ifPresent(timestamp -> subResult.setStampAndTime(timestamp, elapsed));
			}
		});
	}

	protected void setSuccessful(SampleResult subResult, StepResult stepResult) {
		Status status = stepResult.getStatus().orElse(null);
		subResult.setSuccessful(Status.PASSED == status);
	}

	protected void setSuccessful(SampleResult result, MartiniResult martiniResult) {
		Status status = martiniResult.getStatus().orElse(null);
		result.setSuccessful(Status.PASSED == status);
	}

	@Override
	protected void beginTearDown() throws Exception {
	}
}