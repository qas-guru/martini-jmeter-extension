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

package qas.guru.martini.event;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.AfterStepEvent;

@SuppressWarnings("WeakerAccess")
public class DefaultAfterStepEvent extends AbstractStepEvent implements AfterStepEvent {

	protected SampleResult result;

	public SampleResult getResult() {
		return result;
	}

	@Override
	public Martini getMartini() {
		return null;
	}

	@Override
	public Step getStep() {
		return super.getStep();
	}

	@Override
	public boolean isSuccessful() {
		return result.isSuccessful();
	}

	public DefaultAfterStepEvent(Martini martini, Step step, JMeterContext context, SampleResult result) {
		super(result.getEndTime(), martini, step, context);
		this.result = result;
	}

	public DefaultAfterStepEvent(long timestamp, Martini martini, Step step, JMeterContext context) {
		super(timestamp, martini, step, context);
	}
}
