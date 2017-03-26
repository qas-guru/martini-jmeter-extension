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

package qas.guru.martini.jmeter.sampler.gui;

import java.util.Collection;
import java.util.Collections;

import javax.swing.JPopupMenu;

import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.sampler.MartiniSampler;

@SuppressWarnings("WeakerAccess")
public class MartiniSamplerGui extends AbstractMartiniGui {

	private static final long serialVersionUID = 4219666069854254153L;

	public MartiniSamplerGui() throws Exception {
		super();
		initGui();
	}

	@Override
	public TestElement createTestElement() {
		MartiniSampler sampler = new MartiniSampler();
		modifyTestElement(sampler);
		return sampler;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
	}

	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultSamplerMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singletonList(MenuFactory.SAMPLERS);
	}
}
