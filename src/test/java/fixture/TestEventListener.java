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

package fixture;

import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import guru.qas.martini.event.AfterScenarioEvent;
import guru.qas.martini.event.BeforeScenarioEvent;
import guru.qas.martini.event.MartiniEvent;

@Component
class TestEventListener {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(TestEventListener.class);

	@EventListener
	public void handle(MartiniEvent event) {
		LOGGER.info("received event {}", event);
	}

	@EventListener(condition = "!#event.successful")
	public void handleFailedScenario(AfterScenarioEvent event) {
		LOGGER.error("failed scenario: {}", event);
	}

	@EventListener
	public void handleBeforeScenarioEvent(BeforeScenarioEvent event) {
		LOGGER.info("received BeforeScenarioEvent {}", event);
	}
}
