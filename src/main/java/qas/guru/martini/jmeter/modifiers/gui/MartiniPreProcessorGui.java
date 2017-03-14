/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package qas.guru.martini.jmeter.modifiers.gui;

import java.awt.Container;
import java.util.ResourceBundle;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jorphan.gui.layout.VerticalLayout;

import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	protected static final long serialVersionUID = 240L;

	protected static final String RESOURCE_TITLE = "martini_pre_processor_title";
	protected static final String RESOURCE_TEXT_LABEL = "martini_pre_processor_label";
	protected static final String RESOURCE_CONTEXT_CONFIGURATION_LABEL = "martini_context_configuration_label";

	protected ResourceBundle resourceBundle;
	protected JTextField textField;
	protected JTextField contextConfigurationField;

	/**
	 * No-arg constructor.
	 */
	public MartiniPreProcessorGui() {
		super();
		init();
	}

	/**
	 * Handle an error.
	 *
	 * @param e       the Exception that was thrown.
	 * @param thrower the JComponent that threw the Exception.
	 */
	public static void error(Exception e, JComponent thrower) {
		JOptionPane.showMessageDialog(thrower, e, "Error", JOptionPane.ERROR_MESSAGE);
	}

	@Override
	public String getLabelResource() {
		return RESOURCE_TITLE;
	}

	@Override
	public String getStaticLabel() {
		ResourceBundle resourceBundle = getResourceBundle();
		return resourceBundle.getString(RESOURCE_TITLE);
	}

	private ResourceBundle getResourceBundle() {
		if (null == resourceBundle) {
			resourceBundle = ResourceBundle.getBundle("qas.guru.martini.jmeter");
		}
		return resourceBundle;
	}

	/**
	 * Create the test element underlying this GUI component.
	 *
	 * @see org.apache.jmeter.gui.JMeterGUIComponent#createTestElement()
	 */
	@Override
	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
//		super.configureTestElement(preProcessor);
//		preProcessor.setProperty(new StringProperty("text", "Stirred, not shaken."));
//		preProcessor.setProperty(new StringProperty("contextConfiguration", "application.xml"));
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	/**
	 * Modifies a given TestElement to mirror the data in the gui components.
	 *
	 * @see org.apache.jmeter.gui.JMeterGUIComponent#modifyTestElement(TestElement)
	 */
	@Override
	public void modifyTestElement(TestElement preProcessor) {
		super.configureTestElement(preProcessor);
		String text = textField.getText();
		preProcessor.setProperty(new StringProperty("text", text));

		String configuration = contextConfigurationField.getText();
		preProcessor.setProperty(new StringProperty("contextConfiguration", configuration));
	}

	/**
	 * Configure this GUI component from the underlying TestElement.
	 *
	 * @see org.apache.jmeter.gui.JMeterGUIComponent#configure(TestElement)
	 */
	@Override
	public void configure(TestElement el) {
		super.configure(el);

		String text = el.getPropertyAsString("text");
		textField.setText(text);

		String contextConfiguration = el.getPropertyAsString("contextConfiguration");
		contextConfigurationField.setText(contextConfiguration);
	}

	/**
	 * Initialize this component.
	 */
	private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
		resourceBundle = ResourceBundle.getBundle("qas.guru.martini.jmeter");
		initTitlePanel();
		initTextPanel();
		initContextConfigurationPanel();
	}

	protected void initTitlePanel() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));
		setBorder(makeBorder());
		Container container = makeTitlePanel();
		add(container);
	}

	protected void initTextPanel() {
		textField = initTextField(RESOURCE_TEXT_LABEL, "Stirred, not shaken.");
	}

	protected JTextField initTextField(String labelKey, String defaultValue) {
		Box box = Box.createHorizontalBox();
		String label = resourceBundle.getString(labelKey);
		JLabel textLabel = new JLabel(label);
		box.add(textLabel);

		JTextField field = new JTextField(6);
		field.setText(defaultValue);
		box.add(field);
		add(box);
		return field;
	}

	protected void initContextConfigurationPanel() {
		contextConfigurationField = initTextField(RESOURCE_CONTEXT_CONFIGURATION_LABEL, "applicationContext.xml");
	}
}
