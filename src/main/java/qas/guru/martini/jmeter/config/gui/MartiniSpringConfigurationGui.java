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
import javax.swing.BoxLayout;
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

	protected final JTextField contextLocationsField;
	protected final JTextField profilesField;
	protected final SpringArgumentsPanel environmentPanel;

	public MartiniSpringConfigurationGui() throws Exception {
		super();
		contextLocationsField = new JTextField(6);
		profilesField = new JTextField(6);
		environmentPanel = new SpringArgumentsPanel("");
		initGui();
	}

	@Override
	protected void initGui() {
		initTitlePanel();
		JPanel panel = new JPanel(new BorderLayout(0, 5));
		JPanel springPanel = getSpringPanel();
		panel.add(springPanel, BorderLayout.CENTER);
		add(panel, BorderLayout.CENTER);
	}

	protected JPanel getSpringPanel() {
		JPanel panel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(boxLayout);

		Box contextLocationsBox = getContextLocationsBox();
		panel.add(contextLocationsBox);

		Box profilesBox = getProfilesBox();
		panel.add(profilesBox);

		initEnvironmentPanel();
		panel.add(environmentPanel);
		return panel;
	}

	protected Box getContextLocationsBox() {
		setMaximumSize(contextLocationsField);
		String label = super.getDescriptorValue("label.contexts");
		String tooltip = super.getDescriptorValue("tooltip.contexts");
		return getBox(label, tooltip, contextLocationsField);
	}

	protected Box getProfilesBox() {
		setMaximumSize(profilesField);
		String label = super.getDescriptorValue("label.profiles");
		String tooltip = super.getDescriptorValue("tooltip.profiles");
		return getBox(label, tooltip, profilesField);
	}

	protected Box getBox(String label, String tooltip, JTextField field) {
		JLabel jLabel = new JLabel(label);
		jLabel.setToolTipText(tooltip);
		jLabel.setLabelFor(field);
		return getBox(jLabel, field);
	}

	protected Box getBox(JLabel label, JTextField field) {
		Box box = Box.createHorizontalBox();
		box.add(label);
		box.add(field);
		return box;
	}

	protected void initEnvironmentPanel() {
		String label = super.getDescriptorValue("label.environment");
		String tooltip = super.getDescriptorValue("tooltip.environment");
		JLabel jLabel = environmentPanel.getTableLabel();
		jLabel.setText(label);
		jLabel.setToolTipText(tooltip);
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
		String contextLocations = config.getContextLocations();
		contextLocationsField.setText(contextLocations);
		String profiles = config.getProfiles();
		profilesField.setText(profiles);
		Arguments environment = config.getEnvironmentProperties();
		environmentPanel.configure(environment);
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
		TestElement testElement = environmentPanel.createTestElement();
		Arguments arguments = Arguments.class.cast(testElement);
		configuration.setEnvironmentProperties(arguments);

		String locations = contextLocationsField.getText();
		configuration.setContextLocations(locations);

		String profiles = profilesField.getText();
		configuration.setProfiles(profiles);
	}

	@Override
	public void clearGui() {
		super.clearGui();

		String contextsDefault = super.getDescriptorValue("default.contexts");
		contextLocationsField.setText(contextsDefault);

		String profilesDefault = super.getDescriptorValue("default.profiles");
		profilesField.setText(profilesDefault);

		environmentPanel.clearGui();
		Arguments arguments = new Arguments();
		environmentPanel.configure(arguments);
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
