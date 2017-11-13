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

package guru.qas.martini.jmeter.gui;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextArea;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.SpringPreProcessor;

import static guru.qas.martini.jmeter.SpringPreProcessor.ARGUMENT_LOCATIONS;

@SuppressWarnings("unused") // Referenced by JMeter.
public final class SpringPreProcessorGui extends AbstractMartiniPreProcessorGui {

	private final JLabeledTextArea configLocationsField;

	public SpringPreProcessorGui() {
		super("martini_spring_pre_processor_label",
			"Martini Spring PreProcessor",
			"Martini Spring Configuration",
			SpringPreProcessor.class);

		configLocationsField = new JLabeledTextArea("configLocations (comma delimited)");
		init();
	}

	private void init() {
		setBorder(makeBorder());
		setLayout(new VerticalLayout(5, 3));
		add(makeTitlePanel());
		add(configLocationsField);
	}

	@Override
	public TestElement createTestElement() {
		Arguments defaults = SpringPreProcessor.getDefaultArguments();
		return super.createTestElement(defaults);
	}

	@Override
	public void modifyTestElement(TestElement testElement) {
		super.modifyTestElement(testElement);
		String locations = configLocationsField.getText();
		testElement.setProperty(ARGUMENT_LOCATIONS, locations);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		String locations = element.getPropertyAsString(ARGUMENT_LOCATIONS);
		configLocationsField.setText(locations);
	}
}

