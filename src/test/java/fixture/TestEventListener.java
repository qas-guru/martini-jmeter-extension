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

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import qas.guru.martini.event.ScenarioEvent;
import qas.guru.martini.event.SuiteEvent;

@Component
class TestEventListener {

	private static final Logger LOG = LoggingManager.getLoggerFor(TestEventListener.class.getName());

	@EventListener
	public void handle(SuiteEvent event) {
		LOG.info("received event " + event);
	}

	@EventListener
	public void handle(ScenarioEvent event) {
		LOG.info("received event " + event);
	}
}
