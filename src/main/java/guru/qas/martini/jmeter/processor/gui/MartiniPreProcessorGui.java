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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.gui.layout.VerticalLayout;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.processor.MartiniPreProcessor;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class MartiniPreProcessorGui extends AbstractPreProcessorGui {

	protected static final String KEY_SPEL_FILTER_LABEL = "spring.spel.filter.label";
	protected static final String KEY_SHUFFLED_LABEL = "spring.martinis.shuffled";
	protected static final String KEY_RANDOM_SEED_LABEL = "spring.martinis.shuffle.random.seed";

	protected final JTextArea spelFilterField;
	protected final JCheckBox shuffled;
	protected final JTextArea randomSeed;

	public MartiniPreProcessorGui() {
		spelFilterField = new JTextArea("", 2, 6);
		shuffled = new JCheckBox();
		randomSeed = new JTextArea("", 2, 6);
		init();
	}

	protected void init() {
		setBorder(makeBorder());
		setLayout(new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP));

		add(makeTitlePanel());

		style(spelFilterField);
		VerticalPanel filterPanel = getFilterPanel();
		add(filterPanel);

		VerticalPanel shuffledPanel = getShuffledPanel();
		add(shuffledPanel);

		VerticalPanel randomSeedPanel = getRandomSeedPanel();
		add(randomSeedPanel);
	}

	protected VerticalPanel getFilterPanel() {
		VerticalPanel panel = getVerticalPanel(KEY_SPEL_FILTER_LABEL);
		panel.add(spelFilterField);
		return panel;
	}

	protected VerticalPanel getShuffledPanel() {
		VerticalPanel panel = getVerticalPanel(KEY_SHUFFLED_LABEL);
		panel.add(shuffled);
		return panel;
	}

	protected VerticalPanel getRandomSeedPanel() {
		VerticalPanel panel = getVerticalPanel(KEY_RANDOM_SEED_LABEL);
		panel.add(randomSeed);
		return panel;
	}

	@SuppressWarnings("Duplicates")
	protected VerticalPanel getVerticalPanel(String key) {
		VerticalPanel panel = new VerticalPanel();
		MessageSource messageSource = getMessageSource();
		String text = messageSource.getMessage(key, null, key, JMeterUtils.getLocale());
		JLabel label = Gui.getJLabel(null == text ? key : text, 2);
		panel.add(label);
		return panel;
	}

	protected void style(JTextArea area) {
		area.setColumns(6);
		area.setRows(1);
		area.setLineWrap(true);
		area.setBorder(BorderFactory.createEtchedBorder());
	}

	protected MessageSource getMessageSource() {
		return MessageSources.getMessageSource(getClass());
	}

	@Override
	public String getStaticLabel() {
		MessageSource messageSource = getMessageSource();
		String key = getLabelResource();
		return messageSource.getMessage(key, null, key, JMeterUtils.getLocale());
	}

	public String getLabelResource() {
		return "gui.title";
	}

	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	public void modifyTestElement(TestElement element) {
		super.configureTestElement(element);
		MartiniPreProcessor preProcessor = MartiniPreProcessor.class.cast(element);

		String spelFilter = spelFilterField.getText();
		preProcessor.setSpelFilter(spelFilter);

		boolean shuffled = this.shuffled.isSelected();
		preProcessor.setShuffled(shuffled);

		String randomSeed = this.randomSeed.getText();
		preProcessor.setRandomSeed(randomSeed);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniPreProcessor preProcessor = MartiniPreProcessor.class.cast(element);

		String spelFilter = preProcessor.getSpelFilter();
		spelFilterField.setText(spelFilter);
	}

	@Override
	public void clearGui() {
		spelFilterField.setText(MartiniPreProcessor.DEFAULT_SPEL_FILTER);
		super.clearGui();
	}
}
