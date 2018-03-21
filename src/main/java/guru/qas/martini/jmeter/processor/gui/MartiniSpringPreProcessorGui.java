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

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor;

import static guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MartiniSpringPreProcessorGui extends AbstractPreProcessorGui {

	protected static final String KEY_SPRING_LABEL = "spring.panel.label";
	protected static final String KEY_FEATURE_LABEL = "feature.panel.label";
	protected static final String KEY_ENVIRONMENT_LABEL = "spring.environment.label";

	protected final JTextField configLocationsField;
	protected final JTextField featureLocationsField;
	protected final EnvironmentPanel environmentPanel;

	public MartiniSpringPreProcessorGui() {
		configLocationsField = new JTextField(6);
		featureLocationsField = new JTextField(6);
		environmentPanel = new EnvironmentPanel(null, true);
		init();
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		VerticalPanel springPanel = getSpringPanel();
		add(springPanel);

		VerticalPanel featurePanel = getFeaturePanel();
		add(featurePanel);

		VerticalPanel environmentDisplayPanel = getEnvironmentDisplayPanel();
		add(environmentDisplayPanel);
	}

	protected VerticalPanel getSpringPanel() {
		VerticalPanel panel = getVerticalPanel(KEY_SPRING_LABEL);
		configLocationsField.setText(DEFAULT_RESOURCES_CONTEXT);
		panel.add(configLocationsField);
		return panel;
	}

	private VerticalPanel getVerticalPanel(String key) {
		VerticalPanel panel = new VerticalPanel();
		MessageSource messageSource = getMessageSource();
		String text = messageSource.getMessage(key, null, key, JMeterUtils.getLocale());
		JLabel label = Gui.getJLabel(text, 2);
		panel.add(label);
		return panel;
	}

	protected MessageSource getMessageSource() {
		return MessageSources.getMessageSource(getClass());
	}

	protected VerticalPanel getFeaturePanel() {
		VerticalPanel panel = getVerticalPanel(KEY_FEATURE_LABEL);
		featureLocationsField.setText(DEFAULT_RESOURCES_FEATURES);
		panel.add(featureLocationsField);
		return panel;
	}

	protected VerticalPanel getEnvironmentDisplayPanel() {
		VerticalPanel panel = getVerticalPanel(KEY_ENVIRONMENT_LABEL);
		panel.add(environmentPanel);
		return panel;
	}

	@Override
	public String getStaticLabel() {
		MessageSource messageSource = getMessageSource();
		String key = getLabelResource();
		return messageSource.getMessage(key, null, key, JMeterUtils.getLocale());
	}

	public String getLabelResource() {
		return "gui.title";
	}

	public TestElement createTestElement() {
		MartiniSpringPreProcessor preProcessor = new MartiniSpringPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		MartiniSpringPreProcessor preProcessor = MartiniSpringPreProcessor.class.cast(element);

		String configSetting = configLocationsField.getText();
		preProcessor.setConfigLocations(configSetting);

		String featureSetting = featureLocationsField.getText();
		preProcessor.setFeatureLocations(featureSetting);

		Arguments arguments = Arguments.class.cast(environmentPanel.createTestElement());
		preProcessor.setEnvironment(arguments);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniSpringPreProcessor preProcessor = MartiniSpringPreProcessor.class.cast(element);

		String configSetting = preProcessor.getConfigLocations();
		configLocationsField.setText(configSetting);

		String featureSetting = preProcessor.getFeatureLocations();
		featureLocationsField.setText(featureSetting);

		Arguments arguments = preProcessor.getEnvironment();
		if (null != arguments) {
			environmentPanel.configure(arguments);
		}
	}

	@Override
	public void clearGui() {
		environmentPanel.clear();
		configLocationsField.setText(DEFAULT_RESOURCES_CONTEXT);
		featureLocationsField.setText(DEFAULT_RESOURCES_FEATURES);
		super.clearGui();
	}
}
