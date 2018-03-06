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

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.processor.MartiniPreProcessor;

@SuppressWarnings("WeakerAccess")
public final class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	private static final long serialVersionUID = 4447406345771389792L;

	protected static final String DEFAULT = "classpath*:**/contextOne.xml,classpath*:**/contextTwo.xml";

	protected JTextField configLocationsField;

	public MartiniPreProcessorGui() {
		init();
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		Box panel = Box.createHorizontalBox();
		JLabel label = new JLabel("Spring Configuration Locations ");
		panel.add(label);

		configLocationsField = new JTextField(6);
		configLocationsField.setText(DEFAULT);
		panel.add(configLocationsField);

		add(panel);
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
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniPreProcessor preProcessor = MartiniPreProcessor.class.cast(element);
		String setting = preProcessor.getConfigLocations();
		configLocationsField.setText(setting);
	}

	@Override
	public void clearGui() {
		configLocationsField.setText(DEFAULT);
		super.clearGui();
	}

	/*
    public static void error(Exception e, JComponent thrower) {
        JOptionPane.showMessageDialog(thrower, e, "Error", JOptionPane.ERROR_MESSAGE);
	 */
}
