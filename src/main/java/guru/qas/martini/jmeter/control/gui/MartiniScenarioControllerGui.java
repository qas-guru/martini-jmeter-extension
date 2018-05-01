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

package guru.qas.martini.jmeter.control.gui;

import org.apache.jmeter.control.gui.LogicControllerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.control.MartiniScenarioController;

@SuppressWarnings("unused")
public class MartiniScenarioControllerGui extends LogicControllerGui {

	public MartiniScenarioControllerGui() {
		super();
	}

	@Override
	public TestElement createTestElement() {
		MartiniScenarioController controller = new MartiniScenarioController();
		configureTestElement(controller);
		return controller;
	}

	@Override
	public String getStaticLabel() {
		String key = getLabelResource();
		MessageSource messageSource = MessageSources.getMessageSource(getClass());
		return messageSource.getMessage(key, null, JMeterUtils.getLocale());
	}

	@Override
	public String getLabelResource() {
		return "gui.title";
	}
}
