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

import guru.qas.martini.Martini;
import guru.qas.martini.event.BeforeScenarioEvent;
import guru.qas.martini.event.ScenarioIdentifier;

@SuppressWarnings("WeakerAccess")
public class DefaultBeforeScenarioEvent extends AbstractScenarioEvent implements BeforeScenarioEvent {

	@Override
	public ScenarioIdentifier getScenarioIdentifier() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Martini getMartini() {
		return super.getMartini();
	}

	public DefaultBeforeScenarioEvent(Martini martini, JMeterContext context) {
		super(martini, context);
	}
}
