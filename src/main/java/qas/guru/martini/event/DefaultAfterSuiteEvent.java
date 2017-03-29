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

import guru.qas.martini.event.BeforeSuiteEvent;
import guru.qas.martini.event.SuiteIdentifier;

public class DefaultAfterSuiteEvent extends AbstractMartiniEvent implements BeforeSuiteEvent {

	public DefaultAfterSuiteEvent(long timestamp, JMeterContext context) {
		super(timestamp, context);
	}

	@Override
	public SuiteIdentifier getSuiteIdentifier() {
		throw new UnsupportedOperationException();
	}
}
