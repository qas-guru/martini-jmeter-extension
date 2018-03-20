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

import java.awt.Container;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.I18n;
import guru.qas.martini.jmeter.processor.MartiniBeanPreProcessor;
import guru.qas.martini.jmeter.processor.OnError;

import static guru.qas.martini.jmeter.processor.OnError.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MartiniBeanPreProcessorGui extends AbstractPreProcessorGui {

	private static final long serialVersionUID = 4447406345771389792L;

	protected static final String DEFAULT_NAME = "mySetupBean";
	protected static final String DEFAULT_TYPE = "com.mycompany.MyPreProcessorBean";

	protected final JTextField nameField;
	protected final JTextField typeField;
	protected RadiosPanel<OnError> radiosPanel;

	@SuppressWarnings("deprecation")
	public MartiniBeanPreProcessorGui() {
		nameField = new JTextField(6);
		typeField = new JTextField(6);
		init();
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		setBorder(makeBorder());
		add(makeTitlePanel());

		VerticalPanel beanPanel = getBeanPanel();
		add(beanPanel);
	}

	protected VerticalPanel getBeanPanel() {
		VerticalPanel panel = new VerticalPanel();
		addBeanIdentifiers(panel);
		addOnErrorSelection(panel);
		return panel;
	}

	protected void addBeanIdentifiers(Container container) {
		VerticalPanel beanPanel = new VerticalPanel();
		beanPanel.setBorder(BorderFactory.createEtchedBorder());

		JLabel instructions = Gui.getJLabel(getClass(), "panel.instructions", 0);
		Font current = instructions.getFont();
		Font italicized = new Font(current.getName(), Font.ITALIC, current.getSize());
		beanPanel.add(instructions);

		JLabel nameLabel = Gui.getJLabel(getClass(), "preprocessor.bean.name", 1);
		beanPanel.add(nameLabel);

		nameField.setText(DEFAULT_NAME);
		beanPanel.add(nameField);

		JLabel typeLabel = Gui.getJLabel(getClass(), "preprocessor.bean.type", 1);
		beanPanel.add(typeLabel);

		typeField.setText(DEFAULT_TYPE);
		beanPanel.add(typeField);

		container.add(beanPanel);
	}

	protected void addOnErrorSelection(Container container) {
		JLabel label = Gui.getJLabel(getClass(), "on.error.description", 1);
		radiosPanel = new RadiosPanel<>(OnError.class, label);
		radiosPanel.setBorder(BorderFactory.createEtchedBorder());
		radiosPanel.addButton(STOP_TEST, STOP_TEST.getLabel(), true);
		radiosPanel.addButton(STOP_THREAD, STOP_THREAD.getLabel(), false);
		radiosPanel.addButton(PROCEED, PROCEED.getLabel(), false);
		container.add(radiosPanel);
	}

	@Override
	public String getStaticLabel() {
		return I18n.getMessage(getClass(), getLabelResource());
	}

	public String getLabelResource() {
		return "gui.title";
	}

	public TestElement createTestElement() {
		MartiniBeanPreProcessor preProcessor = new MartiniBeanPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		MartiniBeanPreProcessor preProcessor = MartiniBeanPreProcessor.class.cast(element);

		String nameSetting = nameField.getText();
		preProcessor.setBeanName(nameSetting);

		String typeSetting = typeField.getText();
		preProcessor.setBeanType(typeSetting);

		OnError onError = radiosPanel.getSelected().orElse(null);
		preProcessor.setOnError(onError);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniBeanPreProcessor preProcessor = MartiniBeanPreProcessor.class.cast(element);

		String nameSetting = preProcessor.getBeanName();
		nameField.setText(nameSetting);

		String typeSetting = preProcessor.getBeanType();
		typeField.setText(typeSetting);

		OnError onError = preProcessor.getOnError();
		radiosPanel.setSelected(onError);
	}

	@Override
	public void clearGui() {
		nameField.setText(DEFAULT_NAME);
		typeField.setText(DEFAULT_TYPE);
		radiosPanel.setSelected(STOP_TEST);
		super.clearGui();
	}
}