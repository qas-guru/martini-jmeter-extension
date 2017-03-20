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

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.BeforeStepEvent;

@SuppressWarnings("WeakerAccess")
public class DefaultBeforeStepEvent extends AbstractStepEvent implements BeforeStepEvent {

	@Override
	public Martini getMartini() {
		return super.getMartini();
	}

	@Override
	public Step getStep() {
		return null;
	}

	public DefaultBeforeStepEvent(Martini martini, Step step, JMeterContext context) {
		this(System.currentTimeMillis(), martini, step, context);
	}

	public DefaultBeforeStepEvent(long timestamp, Martini martini, Step step, JMeterContext context) {
		super(timestamp, martini, step, context);
	}
}
