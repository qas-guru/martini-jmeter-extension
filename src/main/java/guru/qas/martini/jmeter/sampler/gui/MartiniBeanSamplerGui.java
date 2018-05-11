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

import java.awt.BorderLayout;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.samplers.gui.AbstractSamplerGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;
import guru.qas.martini.jmeter.config.gui.MartiniBeanConfigGui;
import guru.qas.martini.jmeter.sampler.MartiniBeanSampler;

/**
 * Modeled after JavaTestSamplerGui.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanSamplerGui extends AbstractSamplerGui {

	protected final MartiniBeanConfigGui<JavaSamplerClient> configurationPanel;

	public MartiniBeanSamplerGui() {
		super();
		configurationPanel = new MartiniBeanConfigGui<>(JavaSamplerClient.class, false);
		init();
	}

	protected void init() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);
		add(configurationPanel, BorderLayout.CENTER);
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
	public TestElement createTestElement() {
		MartiniBeanSampler sampler = new MartiniBeanSampler();
		modifyTestElement(sampler);
		return sampler;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		element.clear();
		configureTestElement(element);
		MartiniBeanSampler sampler = MartiniBeanSampler.class.cast(element);
		MartiniBeanConfig config = configurationPanel.createTestElement();
		sampler.setConfig(config);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniBeanSampler sampler = MartiniBeanSampler.class.cast(element);
		MartiniBeanConfig config = sampler.getConfig();
		configurationPanel.setConfig(config);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		configurationPanel.clearGui();
	}
}