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

package guru.qas.martini.jmeter.control.gui;

import java.awt.Container;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.gui.AbstractControllerGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.ArgumentPanel;
import guru.qas.martini.jmeter.control.MartiniBeanController;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MartiniBeanControllerGui extends AbstractControllerGui {

	protected final JTextField nameField;
	protected final JTextField typeField;
	protected final ArgumentPanel argumentPanel;

	@SuppressWarnings("deprecation")
	public MartiniBeanControllerGui() {
		super();
		nameField = new JTextField(6);
		typeField = new JTextField(6);
		argumentPanel = new ArgumentPanel(null, true);
		init();
	}

	protected MessageSource getMessageSource() {
		return MessageSources.getMessageSource(getClass());
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		VerticalPanel beanPanel = getBeanPanel();
		add(beanPanel);

		VerticalPanel argumentDisplayPanel = getArgumentDisplayPanel();
		add(argumentDisplayPanel);
	}

	protected VerticalPanel getBeanPanel() {
		VerticalPanel panel = new VerticalPanel();
		addBeanIdentifiers(panel);
		return panel;
	}

	protected void addBeanIdentifiers(Container container) {
		VerticalPanel beanPanel = new VerticalPanel();
		beanPanel.setBorder(BorderFactory.createEtchedBorder());

		MessageSource messageSource = getMessageSource();
		String text = messageSource.getMessage("panel.instructions", null, JMeterUtils.getLocale());
		JLabel instructions = Gui.getJLabel(text, 0);
		Font current = instructions.getFont();
		Font italicized = new Font(current.getName(), Font.ITALIC, current.getSize());
		beanPanel.add(instructions);

		text = messageSource.getMessage("preprocessor.bean.name", null, JMeterUtils.getLocale());
		JLabel nameLabel = Gui.getJLabel(text, 1);
		beanPanel.add(nameLabel);

		nameField.setText(getDefaultBeanName());
		beanPanel.add(nameField);

		text = messageSource.getMessage("preprocessor.bean.type", null, JMeterUtils.getLocale());
		JLabel typeLabel = Gui.getJLabel(text, 1);
		beanPanel.add(typeLabel);

		typeField.setText(getDefaultBeanType());
		beanPanel.add(typeField);

		container.add(beanPanel);
	}

	protected String getDefaultBeanName() {
		MessageSource messageSource = getMessageSource();
		return messageSource.getMessage(
			"martini.bean.controller.default.name", null, "myControllerBean", JMeterUtils.getLocale());
	}

	protected String getDefaultBeanType() {
		MessageSource messageSource = getMessageSource();
		return messageSource.getMessage(
			"martini.bean.controller.default.type", null, "com.mine.MyControllerBean", JMeterUtils.getLocale());
	}

	protected VerticalPanel getArgumentDisplayPanel() {
		VerticalPanel panel = new VerticalPanel();
		String text = getMessageSource().getMessage("panel.arguments", null, "panel.arguments", JMeterUtils.getLocale());
		JLabel label = Gui.getJLabel(text, 2);
		panel.add(label);
		panel.add(argumentPanel);
		return panel;
	}

	@Override
	public String getStaticLabel() {
		String key = getLabelResource();
		return getMessageSource().getMessage(key, null, "Martini Bean Controller", JMeterUtils.getLocale());
	}

	public String getLabelResource() {
		return "gui.title";
	}

	public TestElement createTestElement() {
		MartiniBeanController controller = new MartiniBeanController();
		modifyTestElement(controller);
		return controller;
	}

	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		MartiniBeanController controller = MartiniBeanController.class.cast(element);

		String nameSetting = nameField.getText();
		controller.setBeanName(nameSetting);

		String typeSetting = typeField.getText();
		controller.setBeanType(typeSetting);

		Arguments arguments = Arguments.class.cast(argumentPanel.createTestElement());
		controller.setArguments(arguments);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniBeanController controller = MartiniBeanController.class.cast(element);

		String nameSetting = controller.getBeanName();
		nameField.setText(nameSetting);

		String typeSetting = controller.getBeanType();
		typeField.setText(typeSetting);

		Arguments arguments = controller.getArguments();
		if (null != arguments) {
			argumentPanel.configure(arguments);
		}
	}

	@Override
	public void clearGui() {
		nameField.setText(getDefaultBeanName());
		typeField.setText(getDefaultBeanType());
		argumentPanel.clear();
		super.clearGui();
	}
}