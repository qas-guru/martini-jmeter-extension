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

import java.util.Date;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

public final class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	private static final String RESOURCE_LABEL = "martini_pre_processor_label";
	private static final String RESOURCE_TITLE = "martini_pre_processor_title";
	private static final String DEFAULT_TEXT = "Stirred, not shaken.";

	private static final long serialVersionUID = 240L;

	//private final AtomicReference<ResourceBundle> resourceBundleReference;
	private ResourceBundle resourceBundle;
	private JTextField textField;

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
	}

	/**
	 * Configure this GUI component from the underlying TestElement.
	 *
	 * @see org.apache.jmeter.gui.JMeterGUIComponent#configure(TestElement)
	 */
	@Override
	public void configure(TestElement el) {
		super.configure(el);
		MartiniPreProcessor cast = MartiniPreProcessor.class.cast(el);
		Date timestamp = cast.getTimestamp();
		textField.setText(null == timestamp ? DEFAULT_TEXT : timestamp.toString());
	}

	/**
	 * Initialize this component.
	 */
	private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
		resourceBundle = ResourceBundle.getBundle("qas.guru.martini.jmeter");

		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		Box textPanel = Box.createHorizontalBox();
		//ResourceBundle resourceBundle = getResourceBundle();
		String label = resourceBundle.getString(RESOURCE_LABEL);
		JLabel textLabel = new JLabel(label);
		textPanel.add(textLabel);

		textField = new JTextField(6);
		textField.setText(DEFAULT_TEXT);
		textPanel.add(textField);

		add(textPanel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearGui() {
		textField.setText(DEFAULT_TEXT);
		super.clearGui();
	}
}
