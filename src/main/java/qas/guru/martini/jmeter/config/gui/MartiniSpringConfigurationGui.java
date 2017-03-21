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

package qas.guru.martini.jmeter.config.gui;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.config.MartiniSpringConfiguration;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfigurationGui extends AbstractMartiniGui {

	private static final long serialVersionUID = -4852200848794934491L;

	protected final JTextField contextLocationField;
	protected final SpringArgumentsPanel argumentsPanel;

	public MartiniSpringConfigurationGui() {
		super();
		contextLocationField = new JTextField(6);
		argumentsPanel = new SpringArgumentsPanel("");
		initGui();
	}

	@Override
	protected void initGui() {
		initTitlePanel();

		JPanel panel = new JPanel(new BorderLayout(0, 5));
		Box box = getContextLocationBox();
		panel.add(box, BorderLayout.NORTH);

		panel.add(argumentsPanel, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);
	}

	protected Box getContextLocationBox() {
		String key = String.format("%s.spring.context.label", getClass().getName());
		String value = super.getResourceBundleManager().get(key);
		JLabel jLabel = new JLabel(null == value ? key : value);
		return getContextLocationBox(jLabel);
	}

	protected Box getContextLocationBox(JLabel label) {
		Box box = Box.createHorizontalBox();
		box.add(label);
		box.add(contextLocationField);
		return box;
	}

	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultConfigElementMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singleton(MenuFactory.CONFIG_ELEMENTS);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniSpringConfiguration config = MartiniSpringConfiguration.class.cast(element);
		String contextLocation = config.getConfigLocation();
		contextLocationField.setText(contextLocation);
		Arguments arguments = config.getArguments();
		argumentsPanel.configure(arguments);
	}

	@Override
	public TestElement createTestElement() {
		MartiniSpringConfiguration config = new MartiniSpringConfiguration();
		modifyTestElement(config);
		return config;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		configureTestElement(element);

		MartiniSpringConfiguration configuration = MartiniSpringConfiguration.class.cast(element);
		TestElement testElement = argumentsPanel.createTestElement();
		Arguments arguments = Arguments.class.cast(testElement);
		configuration.setArguments(arguments);

		String location = contextLocationField.getText();
		configuration.setConfigLocation(location);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		argumentsPanel.clearGui();

		String defaultLocation = getDefaultContextLocation();
		contextLocationField.setText(defaultLocation);

		Arguments arguments = new Arguments();
		argumentsPanel.configure(arguments);
	}

	protected String getDefaultContextLocation() {
		String key = String.format("%s.application.context", getClass().getName());
		String value = getResourceBundleManager().get(key);
		return null == value ? key : value;
	}

	protected class SpringArgumentsPanel extends ArgumentsPanel {

		protected SpringArgumentsPanel(String label) {
			super(label);
		}

		@Override
		public JLabel getTableLabel() {
			return super.getTableLabel();
		}
	}
}
