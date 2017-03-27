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

package qas.guru.martini.jmeter.control.gui;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.control.MartiniController;

@SuppressWarnings("WeakerAccess")
public class MartiniControllerGui extends AbstractMartiniGui {

	private static final long serialVersionUID = -1197223648762160415L;

	protected final JTextArea filterField;

	public MartiniControllerGui() throws Exception {
		super();
		filterField = new JTextArea(2, 1);
		filterField.setAlignmentX(0);
		initGui();
	}

	protected void initGui() {
		initTitlePanel();
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel filterPanel = getFilterPanel();
		mainPanel.add(filterPanel, BorderLayout.CENTER);
		add(mainPanel, BorderLayout.CENTER);
	}

	protected JPanel getFilterPanel() {
		JPanel panel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
		panel.setLayout(boxLayout);

		setMaximumSize(filterField);

		Box box = Box.createVerticalBox();
		String label = super.getDescriptorValue("label.filter");
		String tooltip = super.getDescriptorValue("tooltip.filter");
		JLabel jLabel = new JLabel(label, SwingConstants.LEFT);
		jLabel.setToolTipText(tooltip);
		jLabel.setLabelFor(filterField);
		box.add(jLabel);
		box.add(filterField);

		panel.add(box);
		return panel;
	}

	@Override
	public TestElement createTestElement() {
		MartiniController controller = new MartiniController();
		modifyTestElement(controller);
		return controller;
	}

	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultControllerMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singleton(MenuFactory.CONTROLLERS);
	}

	@Override
	public void configure(TestElement element) {
		super.configureTestElement(element);
		MartiniController controller = MartiniController.class.cast(element);
		String filter = controller.getFilter();
		filterField.setText(filter);
	}

	@Override
	public void modifyTestElement(TestElement element) {
		configureTestElement(element);

		MartiniController controller = MartiniController.class.cast(element);
		String filter = filterField.getText();
		controller.setFilter(filter);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		filterField.setText("");
	}
}
