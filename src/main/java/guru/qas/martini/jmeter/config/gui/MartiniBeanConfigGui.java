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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.JLabeledChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.ArgumentPanel;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Modeled after JavaConfigGui.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanConfigGui<T> extends AbstractConfigGui implements ChangeListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniBeanConfigGui.class);

	protected final MartiniBeanConfig config;
	protected Class<T> implementation;
	protected boolean standalone;
	protected ArgumentPanel argumentPanel;
	protected JLabel warningLabel;
	protected JLabeledChoice implementationChoice;

	public MartiniBeanConfigGui() {
		this(null, false);
	}

	public MartiniBeanConfigGui(Class<T> implementation) {
		this(implementation, false);
	}

	public MartiniBeanConfigGui(Class<T> implementation, boolean standalone) {
		super();
		config = new MartiniBeanConfig();
		this.implementation = implementation;
		this.standalone = standalone;
		init();
	}

	protected void init() {
		String label = JMeterUtils.getResString("paramtable");
		argumentPanel = new ArgumentPanel(label, null, true, false);

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
		implementationChoice = new JLabeledChoice(label, new String[]{null}, true, false);
		implementations.forEach(i -> implementationChoice.addValue(i));
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
			List<T> candidates = getRegisteredCandidates();
			candidates.stream()
				.map(c -> c.getClass().getName())
				.forEach(implementations::add);
		}
		catch (Exception e) {
			LOGGER.warn("unable to find {} implementations registered with ServiceLoader", implementation, e);
		}
		return implementations;
	}

	protected List<T> getRegisteredCandidates() {
		List<T> candidates = null;
		if (null != implementation) {
			ServiceLoader<T> serviceLoader = ServiceLoader.load(implementation);
			candidates = Lists.newArrayList(serviceLoader.iterator());
		}
		return null == candidates ? ImmutableList.of() : candidates;
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

	protected T getDefaultInstance() {
		String className = implementationChoice.getText();
		T configuredInstance = null;
		if (null != implementation && null != className && !className.isEmpty()) {
			try {
				Class<?> chosen = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
				Object o = chosen.newInstance();
				configuredInstance = this.implementation.cast(o);
			}
			catch (Exception e) {
				throw new IllegalStateException("unable to load class of type " + className, e);
			}
		}
		return configuredInstance;
	}

	protected void configureClassName() {
		try {
			T defaultInstance = getDefaultInstance();
			Arguments arguments;
			if (null == defaultInstance) {
				arguments = new Arguments();
			}
			else {
				arguments = getDefaultParameters(defaultInstance);
				Argument defaultBeanNameArgument = config.getDefaultBeanNameArgument();
				if (!arguments.getArgumentsAsMap().containsKey(defaultBeanNameArgument.getName())) {
					ArrayList<Argument> argumentList = Lists.newArrayListWithExpectedSize(arguments.getArgumentCount() + 1);
					argumentList.add(defaultBeanNameArgument);
					PropertyIterator i = arguments.getArguments().iterator();
					Streams.stream(i).map(JMeterProperty::getObjectValue)
						.filter(Argument.class::isInstance)
						.map(Argument.class::cast)
						.forEach(argumentList::add);
					arguments.setArguments(argumentList);
				}
			}
			argumentPanel.configure(arguments);
			warningLabel.setVisible(false);
		}
		catch (Exception e) {
			String className = implementationChoice.getText().trim();
			LOGGER.warn("unable to obtain arguments for {}", className, e);
			warningLabel.setVisible(true);
		}
	}

	protected Arguments getDefaultParameters(Object o) throws InvocationTargetException, IllegalAccessException {
		Method method = getDefaultParametersMethod(o);
		Arguments arguments = null == method ? new Arguments() : (Arguments) method.invoke(o);
		return null == arguments ? new Arguments() : Arguments.class.cast(arguments.clone());
	}

	protected Method getDefaultParametersMethod(Object o) {
		Method[] methods = o.getClass().getMethods();

		return Stream.of(methods)
			.filter(m -> 0 == m.getParameterCount())
			.filter(m -> "getDefaultParameters".equals(m.getName()))
			.filter(m -> Arguments.class.isAssignableFrom(m.getReturnType()))
			.findFirst()
			.orElse(null);
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
		beanConfig.setParameters(arguments);
		String beanType = implementationChoice.getText();
		beanConfig.setBeanType(beanType);
	}

	public void setConfig(MartiniBeanConfig config) {
		checkNotNull(config, "null MartiniBeanConfig");
		Arguments cloned = Arguments.class.cast(config.getParameters().clone());
		this.config.setParameters(cloned);
		implementationChoice.setText(config.getBeanType());
		argumentPanel.configure(cloned);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		implementationChoice.setText(null);
		argumentPanel.clearGui();
	}
}
