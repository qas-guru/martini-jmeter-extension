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

package guru.qas.martini.jmeter.config.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import com.google.common.collect.Lists;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.ArgumentPanel;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;

/**
 * Modeled after JavaConfigGui.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanConfigGui extends AbstractConfigGui implements ChangeListener {

	private static final long serialVersionUID = -308150274609539850L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanConfigGui.class);

	protected final boolean standalone;
	protected ArgumentPanel argumentPanel;
	protected JLabel warningLabel;
	protected JLabeledChoice implementationChoice;

	public MartiniBeanConfigGui() {
		this(false);
	}

	public MartiniBeanConfigGui(boolean standalone) {
		super();
		this.standalone = standalone;
		init();
	}

	protected void init() {
		String argumentsPanelLabel = JMeterUtils.getResString("paramtable");
		argumentPanel = new ArgumentPanel(argumentsPanelLabel, null, true, false);

		ImageIcon image = JMeterUtils.getImage("warning.png");
		warningLabel = new JLabel(JMeterUtils.getResString("java_request_warning"), image, SwingConstants.LEFT);

		setLayout(new BorderLayout(0, 5));

		if (standalone) {
			setBorder(makeBorder());
			add(makeTitlePanel(), BorderLayout.NORTH);
		}

		JPanel classnameRequestPanel = new JPanel(new BorderLayout(0, 5));
		classnameRequestPanel.add(createBeanPanel(), BorderLayout.NORTH);
		classnameRequestPanel.add(argumentPanel, BorderLayout.CENTER);
		configureClassName();
		add(classnameRequestPanel, BorderLayout.CENTER);
	}

	protected JPanel createBeanPanel() {
		SortedSet<String> implementations = getBeanImplementations();
		String label = JMeterUtils.getResString("protocol_java_classname");
		implementationChoice = new JLabeledChoice(label, implementations.toArray(ArrayUtils.EMPTY_STRING_ARRAY), true, false);
		implementationChoice.addChangeListener(this);

		VerticalPanel panel = new VerticalPanel();
		panel.add(implementationChoice);

		warningLabel.setForeground(Color.RED);
		Font font = warningLabel.getFont();
		warningLabel.setFont(new Font(font.getFontName(), Font.BOLD, (int) (font.getSize() * 1.1)));
		warningLabel.setVisible(false);
		panel.add(warningLabel);

		return panel;
	}

	protected SortedSet<String> getBeanImplementations() {
		final SortedSet<String> implementations = new TreeSet<>();
		try {
			List<JavaSamplerClient> candidates = getRegisteredClients();
			candidates.forEach(s -> {
				String className = s.getClass().getName();
				implementations.add(className);
			});
		}
		catch (Exception e) {
			LOGGER.warn("unable to find JavaSamplerClient implementations registered with ServiceLoader", e);
		}
		return implementations;
	}

	protected List<JavaSamplerClient> getRegisteredClients() {
		ServiceLoader<JavaSamplerClient> serviceLoader = ServiceLoader.load(JavaSamplerClient.class);
		return Lists.newArrayList(serviceLoader.iterator());
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource() == implementationChoice) {
			configureClassName();
		}
	}

	public void setConfiguration(Arguments arguments) {
		Map<String, String> index = arguments.getArgumentsAsMap();
		argumentPanel.configure(arguments);
	}

	public void setBeanType(String beanType) {
		implementationChoice.setText(beanType);
	}

	protected JavaSamplerClient getConfiguredClientInstance() {
		String className = implementationChoice.getText();
		JavaSamplerClient instance = null;
		if (null != className && !className.isEmpty()) {
			try {
				Class<?> implementation = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
				Object o = implementation.newInstance();
				instance = JavaSamplerClient.class.cast(o);
			}
			catch (Exception e) {
				throw new IllegalStateException("unable to load class of type " + className, e);
			}
		}
		return instance;
	}

	protected void configureClassName() {
		try {
			Arguments updated = new Arguments();
			JavaSamplerClient client = getConfiguredClientInstance();
			if (null != client) {
				Arguments current = new Arguments();
				argumentPanel.modifyTestElement(current);

				Arguments clientParameters = client.getDefaultParameters();
				if (clientParameters != null) {
					Map<String, String> index = current.getArgumentsAsMap();

					for (JMeterProperty jMeterProperty : clientParameters.getArguments()) {
						Argument arg = (Argument) jMeterProperty.getObjectValue();
						String name = arg.getName();
						String value = arg.getValue();
						String metadata = arg.getMetaData();
						String description = arg.getDescription();

						// If a user has set parameters in one test, and then
						// selects a different test which supports the same
						// parameters, those parameters should have the same
						// values that they did in the original test.
						if (index.containsKey(name)) {
							String newVal = index.get(name);
							if (newVal != null && newVal.length() > 0) {
								value = newVal;
							}
						}
						updated.addArgument(name, value, metadata, description);
					}
				}
			}
			argumentPanel.configure(updated);
			warningLabel.setVisible(false);
		}
		catch (Exception e) {
			String className = implementationChoice.getText().trim();
			LOGGER.error("Error getting argument list for " + className, e);
			warningLabel.setVisible(true);
		}
	}

	@Override
	public String getStaticLabel() {
		MessageSource messageSource = getMessageSource();
		String key = getLabelResource();
		return messageSource.getMessage(key, null, JMeterUtils.getLocale());
	}

	protected MessageSource getMessageSource() {
		return MessageSources.getMessageSource(getClass());
	}

	@Override
	public String getLabelResource() {
		return "gui.title";
	}

	@Override
	public MartiniBeanConfig createTestElement() {
		MartiniBeanConfig config = new MartiniBeanConfig();
		modifyTestElement(config);
		return config;
	}

	@Override
	public void modifyTestElement(TestElement config) {
		configureTestElement(config);
		Arguments arguments = Arguments.class.cast(argumentPanel.createTestElement());
		MartiniBeanConfig beanConfig = MartiniBeanConfig.class.cast(config);
		beanConfig.setArguments(arguments);
		String beanType = implementationChoice.getText();
		beanConfig.setBeanType(beanType);
	}
}
