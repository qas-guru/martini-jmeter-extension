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

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.jmeter.control.gui.AbstractControllerGui;
import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.testelement.TestElement;

import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.I18n;
import guru.qas.martini.jmeter.control.MartiniController;

@SuppressWarnings("WeakerAccess")
public class MartiniControllerPanel extends AbstractControllerGui {

	private final boolean standalone;
	private final JTextArea textArea;

	public MartiniControllerPanel() {
		this(true);
	}

	public MartiniControllerPanel(boolean standalone) {
		this.standalone = standalone;
		this.textArea = new JTextArea(5, 20);
		init();
	}

	private void init() {
		textArea.setEditable(true);

		VerticalPanel spelPanel = getSpelPanel();
		if (standalone) {
			setLayout(new BorderLayout(0, 5));
			setBorder(makeBorder());
			add(makeTitlePanel(), BorderLayout.NORTH);

			JPanel mainPanel = new JPanel(new BorderLayout());
			mainPanel.add(spelPanel, BorderLayout.NORTH);
			add(mainPanel, BorderLayout.CENTER);
		}
		else {
			setLayout(new BorderLayout());
			add(spelPanel, BorderLayout.NORTH);
		}
	}

	private VerticalPanel getSpelPanel() {
		VerticalPanel panel = new VerticalPanel();
		JLabel label = Gui.getJLabel(getClass(), "spel.filter.label", 2);
		panel.add(label);

		JScrollPane scrollPane = new JScrollPane(textArea);
		panel.add(scrollPane);

		return panel;
	}

	@Override
	public String getLabelResource() {
		return "gui.title";
	}

	@Override
	public String getStaticLabel() {
		return I18n.getMessage(getClass(), getLabelResource());
	}

	@Override
	public TestElement createTestElement() {
		MartiniController controller = new MartiniController();
		modifyTestElement(controller);
		return controller;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		if (MartiniController.class.isInstance(element)) {
			MartiniController controller = MartiniController.class.cast(element);
			String text = textArea.getText();
			controller.setSpelFilter(text);
		}
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		if (MartiniController.class.isInstance(element)) {
			MartiniController controller = MartiniController.class.cast(element);
			String filter = controller.getSpelFilter();
			textArea.setText(filter);
		}
	}

	@Override
	public void clearGui() {
		textArea.setText("");
		super.clearGui();
	}
}
