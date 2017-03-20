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

import guru.qas.martini.Martini;
import guru.qas.martini.event.AfterScenarioEvent;

@SuppressWarnings("WeakerAccess")
public class DefaultAfterScenarioEvent extends AbstractScenarioEvent implements AfterScenarioEvent {

	protected final SampleResult result;

	@Override
	public Martini getMartini() {
		return super.getMartini();
	}

	public SampleResult getSampleResult() {
		return result;
	}

	public DefaultAfterScenarioEvent(
		Martini martini,
		JMeterContext context,
		SampleResult result
	) {
		super(result.getEndTime(), martini, context);
		this.result = result;
	}

	@Override
	public boolean isSuccessful() {
		return result.isSuccessful();
	}
}
