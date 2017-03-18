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

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.control.MartiniController;

@SuppressWarnings("WeakerAccess")
public class MartiniControllerGui extends AbstractMartiniGui {

	private static final long serialVersionUID = 240L;

	public MartiniControllerGui() {
		super();
		initGui();
	}

	protected void initGui() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);

		JPanel mainPanel = new JPanel(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
	}

	@Override
	public TestElement createTestElement() {
		MartiniController controller = new MartiniController();
		modifyTestElement(controller);
		return controller;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		if (isMartiniController(element)) {
			super.configureTestElement(element);
		}
	}

	protected boolean isMartiniController(TestElement element) {
		return MartiniController.class.isInstance(element);
	}

	public void configure(TestElement element) {
		if (isMartiniController(element)) {
			super.configure(element);
		}
	}



	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultControllerMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singleton(MenuFactory.CONTROLLERS);
	}
}
