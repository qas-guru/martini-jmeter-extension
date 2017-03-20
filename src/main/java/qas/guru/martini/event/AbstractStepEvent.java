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

import org.apache.jmeter.threads.JMeterContext;

import com.google.common.base.MoreObjects;

import gherkin.ast.Step;
import guru.qas.martini.Martini;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractStepEvent extends AbstractMartiniEvent {

	protected final Martini martini;
	protected final Step step;

	protected Martini getMartini() {
		return martini;
	}

	protected Step getStep() {
		return step;
	}

	protected AbstractStepEvent(
		long timestamp,
		Martini martini,
		Step step,
		JMeterContext context
	) {
		super(timestamp, context);
		this.martini = martini;
		this.step = step;
	}

	@Override
	protected MoreObjects.ToStringHelper getStringHelper() {
		return MoreObjects.toStringHelper(this)
			.add("timestamp", super.getTimestamp())
			.add("context", getJMeterContextStringHelper())
			.add("martini", martini)
			.add("step", step);
	}
}
