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

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.SpringPreProcessor;

@SuppressWarnings("unused") // Referenced by JMeter.
public final class SpringPreProcessorGui extends AbstractPreProcessorGui {

	private final JLabeledTextField configLocationsField;

	public SpringPreProcessorGui() {
		configLocationsField = new JLabeledTextField("configLocations (comma delimited)");
		configLocationsField.setText(SpringPreProcessor.DEFAULT_CONFIG_LOCATIONS);
		init();
	}

	private void init() {
		super.setName(getStaticLabel());
		super.setComment("Spring ClassPathXmlApplicationContext Configuration");
		setBorder(makeBorder());
		setLayout(new VerticalLayout(5, 3));
		add(makeTitlePanel());
		add(configLocationsField);
	}

	@Override
	public String getStaticLabel() {
		return "Martini Spring PreProcessor";
	}

	@Override
	public String getLabelResource() {
		return "spring_pre_processor_label";
	}

	@Override
	public TestElement createTestElement() {
		SpringPreProcessor preProcessor = new SpringPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	@Override
	public void modifyTestElement(TestElement testElement) {
		if (SpringPreProcessor.class.isInstance(testElement)) {
			SpringPreProcessor preProcessor = SpringPreProcessor.class.cast(testElement);
			modifyTestElement(preProcessor);
		}
		configureTestElement(testElement);
	}

	private void modifyTestElement(SpringPreProcessor preProcessor) {
		String configLocations = configLocationsField.getText();
		preProcessor.setProperty(SpringPreProcessor.KEY_CONFIG_LOCATIONS, configLocations);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		String configLocations = element.getPropertyAsString(SpringPreProcessor.KEY_CONFIG_LOCATIONS, "");
		configLocationsField.setText(configLocations);
	}
}

