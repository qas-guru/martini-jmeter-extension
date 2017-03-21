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
import java.awt.Dimension;
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

	public MartiniSpringConfigurationGui() {
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
			return getBox("%s.contexts.label", "%s.contexts.tooltip", contextLocationsField);
		}

		protected void setMaximumSize(JTextField field) {
			Dimension preferredSize = field.getPreferredSize();
			double height = preferredSize.getHeight();
			int maximumHeight = new Double(height).intValue();
			Dimension maximumSize = new Dimension(Integer.MAX_VALUE, maximumHeight);
			field.setMaximumSize(maximumSize);
		}

		protected Box getProfilesBox() {
			setMaximumSize(profilesField);
			return getBox("%s.profiles.label", "%s.profiles.tooltip", profilesField);
		}

		protected Box getBox(String labelKeyTemplate, String tooltipKeyTemplate, JTextField field) {
			String label = getImplementationResource(labelKeyTemplate);
			JLabel jLabel = new JLabel(label);
			String tooltip = super.getImplementationResource(tooltipKeyTemplate);
			jLabel.setToolTipText(tooltip);
			return getBox(jLabel, field);
		}

		protected Box getBox(JLabel label, JTextField field) {
			Box box = Box.createHorizontalBox();
			box.add(label);
			box.add(field);
			return box;
		}

		protected void initEnvironmentPanel() {
			String label = getImplementationResource("%s.environment.label");
			String tooltip = getImplementationResource("%s.environment.tooltip");
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
		configuration.setEnvironment(arguments);

		String locations = contextLocationsField.getText();
		configuration.setContextLocations(locations);

		String profiles = profilesField.getText();
		configuration.setProfiles(profiles);
	}

	@Override
	public void clearGui() {
		super.clearGui();

		String contextsDefault = getImplementationResource("%s.contexts.default");
		contextLocationsField.setText(contextsDefault);

		String profilesDefault = getImplementationResource("%s.profiles.default");
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
