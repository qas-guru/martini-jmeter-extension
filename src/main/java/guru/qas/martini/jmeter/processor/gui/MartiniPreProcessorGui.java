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

package guru.qas.martini.jmeter.processor.gui;

import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.processor.MartiniPreProcessor;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	private static final long serialVersionUID = 4447406345771389792L;

	protected static final String DEFAULT = "classpath*:**/contextOne.xml,classpath*:**/contextTwo.xml";

	protected final JTextField configLocationsField;
	protected final EnvironmentPanel environmentPanel;

	public MartiniPreProcessorGui() {
		configLocationsField = new JTextField(6);
		environmentPanel = new EnvironmentPanel(null, true);
		init();
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		VerticalPanel springPanel = new VerticalPanel();
		JLabel springLabel = new JLabel("Spring Configuration Locations");
		Font springLabelFont = springLabel.getFont();
		springLabel.setFont(springLabelFont.deriveFont((float) springLabelFont.getSize() + 2));
		springPanel.add(springLabel);

		configLocationsField.setText(DEFAULT);
		springPanel.add(configLocationsField);
		add(springPanel);

		VerticalPanel environmentDisplayPanel = new VerticalPanel();
		JLabel environmentLabel = new JLabel("System Properties");
		Font environmentLabelFont = environmentLabel.getFont();
		environmentLabel.setFont(environmentLabelFont.deriveFont((float) environmentLabelFont.getSize() + 2));
		environmentDisplayPanel.add(environmentLabel);

		environmentDisplayPanel.add(environmentPanel);
		add(environmentDisplayPanel);
	}

	@Override
	public String getStaticLabel() {
		return "Martini PreProcessor";
	}

	public String getLabelResource() {
		return "martini_preprocessor_title";
	}

	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		String setting = configLocationsField.getText();
		MartiniPreProcessor preProcessor = MartiniPreProcessor.class.cast(element);
		preProcessor.setConfigLocations(setting);
		Arguments arguments = Arguments.class.cast(environmentPanel.createTestElement());
		preProcessor.setEnvironment(arguments);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniPreProcessor preProcessor = MartiniPreProcessor.class.cast(element);
		String setting = preProcessor.getConfigLocations();
		configLocationsField.setText(setting);
		Arguments arguments = preProcessor.getEnvironment();
		if (null != arguments) {
			environmentPanel.configure(arguments);
		}
	}

	@Override
	public void clearGui() {
		environmentPanel.clear();
		configLocationsField.setText(DEFAULT);
		super.clearGui();
	}
}
