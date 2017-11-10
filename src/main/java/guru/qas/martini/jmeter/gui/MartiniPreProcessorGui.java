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

import javax.swing.JCheckBox;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jorphan.gui.JLabeledTextField;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.MartiniPreProcessor;

import static guru.qas.martini.jmeter.MartiniPreProcessor.*;

@SuppressWarnings("unused") // Referenced by JMeter.
public final class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	private final JCheckBox cyclicCheckbox;
	private final JCheckBox shuffledCheckbox;
	private final JLabeledTextField spelFilterField;

	public MartiniPreProcessorGui() {
		cyclicCheckbox = new JCheckBox("Cyclic Iteration");
		shuffledCheckbox = new JCheckBox("Shuffled Iteration");
		spelFilterField = new JLabeledTextField("SpEL Filter");
		init();
	}

	private void init() {
		setBorder(makeBorder());
		setLayout(new VerticalLayout(5, 3));
		add(makeTitlePanel());

		add(cyclicCheckbox);
		add(shuffledCheckbox);
		add(spelFilterField);
	}

	@Override
	public String getStaticLabel() {
		return "Martini Scenario PreProcessor";
	}

	@Override
	public String getLabelResource() {
		return "martini_pre_processor_label";
	}

	@Override
	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
		preProcessor.setName(getStaticLabel());
		preProcessor.setComment("Martini Scenario Configuration");
		super.configureTestElement(preProcessor);

		Arguments defaults = MartiniPreProcessor.getDefaultArguments();
		for (JMeterProperty property : defaults) {
			Object o = property.getObjectValue();
			Argument argument = Argument.class.cast(o);
			String name = argument.getName();
			String value = argument.getValue();
			preProcessor.setProperty(name, value);
		}
		configure(preProcessor);
		return preProcessor;
	}

	@Override
	public void modifyTestElement(TestElement testElement) {
		boolean cyclic = cyclicCheckbox.isSelected();
		testElement.setProperty(ARGUMENT_CYCLIC_ITERATOR, cyclic);

		boolean shuffled = shuffledCheckbox.isSelected();
		testElement.setProperty(ARGUMENT_SHUFFLED_ITERATION, shuffled);

		String spelFilter = spelFilterField.getText();
		testElement.setProperty(ARGUMENT_SPEL_FILTER, spelFilter);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);

		boolean cyclic = element.getPropertyAsBoolean(ARGUMENT_CYCLIC_ITERATOR);
		cyclicCheckbox.setSelected(cyclic);

		boolean shuffled = element.getPropertyAsBoolean(ARGUMENT_SHUFFLED_ITERATION);
		shuffledCheckbox.setSelected(shuffled);

		String spelFilter = element.getPropertyAsString(ARGUMENT_SPEL_FILTER);
		spelFilterField.setText(spelFilter);
	}

	@Override
	public void clearGui() {
		super.clearGui();
	}
}

