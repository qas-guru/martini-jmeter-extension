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

import java.awt.BorderLayout;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;
import guru.qas.martini.jmeter.config.gui.MartiniBeanConfigGui2;
import guru.qas.martini.jmeter.processor.MartiniBeanPreProcessor;

@SuppressWarnings({"unused", "WeakerAccess"})
public class MartiniBeanPreProcessorGui extends AbstractPreProcessorGui {

	protected MartiniBeanConfigGui2<PreProcessor> configurationPanel;

	public MartiniBeanPreProcessorGui() {
		super();
		init();
	}

	protected void init() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);
		configurationPanel = new MartiniBeanConfigGui2<>(PreProcessor.class, false);
		add(configurationPanel, BorderLayout.CENTER);
	}

	@Override
	public String getStaticLabel() {
		MessageSource messageSource = MessageSources.getMessageSource(getClass());
		String key = getLabelResource();
		return messageSource.getMessage(key, null, key, JMeterUtils.getLocale());
	}

	public String getLabelResource() {
		return "gui.title";
	}

	@Override
	public TestElement createTestElement() {
		MartiniBeanPreProcessor preProcessor = new MartiniBeanPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		element.clear();
		configureTestElement(element);

		MartiniBeanPreProcessor preProcessor = MartiniBeanPreProcessor.class.cast(element);
		MartiniBeanConfig config = configurationPanel.createTestElement();
		Arguments arguments = config.getArguments();
		preProcessor.setArguments(arguments);
		String beanType = config.getBeanType();
		preProcessor.setBeanType(beanType);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniBeanPreProcessor preProcessor = MartiniBeanPreProcessor.class.cast(element);
		Arguments arguments = preProcessor.getArguments();
		configurationPanel.setConfiguration(arguments);
		String beanType = preProcessor.getBeanType();
		configurationPanel.setBeanType(beanType);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		configurationPanel.clearGui();
	}
}
