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
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static qas.guru.martini.jmeter.modifiers.MartiniConstants.PROPERTY_KEY_SPRING_CONFIGURATION;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	protected static final long serialVersionUID = 240L;

	protected static final Logger LOG = LoggingManager.getLoggerFor(MartiniPreProcessorGui.class.getName());
	protected static final String RESOURCE_BUNDLE = "qas.guru.martini.jmeter";
	protected static final String RESOURCE_TITLE = "martini_pre_processor_title";
	protected static final String SPRING_CONFIGURATION_LABEL_RESOURCE = "martini_spring_context_label";

	protected final ResourceBundle resourceBundle;
	protected final JTextField springContextField;

	public MartiniPreProcessorGui() {
		super();
		resourceBundle = getResourceBundle();
		springContextField = new JTextField(6);
		initGui();
	}

	protected ResourceBundle getResourceBundle() {
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE);
		}
		catch (Exception e) {
			String message = String.format("unable to load resource bundle %s", RESOURCE_BUNDLE);
			LOG.warn(message, e);
			JOptionPane.showMessageDialog(this, message + "; see log for details", "Warning", WARNING_MESSAGE);
		}
		return bundle;
	}

	protected void initGui() {
		initBorder();
		initTitlePanel();
		initSpringBox();
	}

	protected void initBorder() {
		Border border = makeBorder();
		setBorder(border);
	}

	protected void initTitlePanel() {
		initTitlePanelLayout();
		initTitlePanelContainer();
	}

	protected void initTitlePanelLayout() {
		VerticalLayout layout = new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP);
		setLayout(layout);
	}

	protected void initTitlePanelContainer() {
		Container container = makeTitlePanel();
		add(container);
	}

	protected void initSpringBox() {
		String label = null == resourceBundle ? "" : resourceBundle.getString(SPRING_CONFIGURATION_LABEL_RESOURCE);
		JLabel jLabel = new JLabel(label);
		initSpringBox(jLabel);
	}

	protected void initSpringBox(JLabel label) {
		Box box = Box.createHorizontalBox();
		box.add(label);
		initSpringBox(box);
	}

	protected void initSpringBox(Box box) {
		box.add(springContextField);
		add(box);
	}

	@Override
	public String getLabelResource() {
		return RESOURCE_TITLE;
	}

	@Override
	public String getStaticLabel() {
		return null == resourceBundle ? "" : resourceBundle.getString(RESOURCE_TITLE);
	}

	@Override
	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	@Override
	public void modifyTestElement(TestElement preProcessor) {
		super.configureTestElement(preProcessor);
		String configuration = springContextField.getText();
		preProcessor.setProperty(PROPERTY_KEY_SPRING_CONFIGURATION, configuration);
	}

	@Override
	public void configure(TestElement el) {
		super.configure(el);

		String contextConfiguration = el.getPropertyAsString(PROPERTY_KEY_SPRING_CONFIGURATION);
		springContextField.setText(contextConfiguration);
	}
}
