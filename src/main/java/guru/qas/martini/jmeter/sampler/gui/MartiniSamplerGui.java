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

package guru.qas.martini.jmeter.sampler.gui;

import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import guru.qas.martini.jmeter.Il8n;
import guru.qas.martini.jmeter.sampler.MartiniSampler;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniSamplerGui extends AbstractSamplerGui {

	private static final long serialVersionUID = -1472260817829729851L;

	public MartiniSamplerGui() {
		init();
	}

	protected void init() {
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));
		setBorder(makeBorder());
		add(makeTitlePanel());
	}

	@Override
	public String getStaticLabel() {
		return Il8n.getMessage(getClass(), getLabelResource());
	}

	@Override
	public String getLabelResource() {
		return "gui.title";
	}

	@Override
	public TestElement createTestElement() {
		MartiniSampler sampler = new MartiniSampler();
		modifyTestElement(sampler);
		return sampler;
	}

	// Sets fields on element.
	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
	}

	// Sets fields on GUI.
	@Override
	public void configure(TestElement element) {
		super.configure(element);
	}

	@Override
	public void clearGui() {
		super.clearGui();
	}
}
