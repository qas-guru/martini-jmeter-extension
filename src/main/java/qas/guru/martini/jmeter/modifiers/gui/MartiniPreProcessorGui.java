/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package qas.guru.martini.jmeter.modifiers.gui;

import java.awt.Container;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.layout.VerticalLayout;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

import static qas.guru.martini.jmeter.modifiers.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessorGui extends AbstractMartiniGui {

	private static final long serialVersionUID = 240L;

	// TODO: replace this mess by distributing as a Plugin for use by PluginManager.
	private static final AtomicBoolean ICONS_REGISTERED = new AtomicBoolean(false);
	private static final String ICON_ENABLED = "icons/spring-logo.png";
	private static final String ICON_DISABLED = "icons/spring-logo-disabled.png";

	protected static final String GUI_TITLE_KEY = "martini_pre_processor_title";
	protected static final String SPRING_CONFIGURATION_LABEL_RESOURCE = "martini_spring_context_label";

	protected final JTextField springContextField;

	public MartiniPreProcessorGui() {
		this(GUI_TITLE_KEY);
	}

	protected MartiniPreProcessorGui(String titleKey) {
		super(titleKey);
		springContextField = new JTextField(6);
		initGui();
	}

	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultExtractorMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singleton(MenuFactory.PRE_PROCESSORS);
	}

	@Override
	protected void initGui() {
		initIcons();
		initBorder();
		initTitlePanel();
		initSpringBox();
	}

	protected void initIcons() {
		synchronized (ICONS_REGISTERED) {
			if (!ICONS_REGISTERED.get()) {
				ICONS_REGISTERED.set(true);
				registerEnabledIcon(ICON_ENABLED);
				registerDisabledIcon(ICON_DISABLED);
			}
		}
	}

	protected void initBorder() {
		Border border = makeBorder();
		setBorder(border);
	}

	protected void initTitlePanel() {
		initTitlePanelLayout();
		initTitlePanelContainer();
	}

	protected void initTitlePanelLayout() {
		VerticalLayout layout = new VerticalLayout(5, VerticalLayout.BOTH, VerticalLayout.TOP);
		setLayout(layout);
	}

	protected void initTitlePanelContainer() {
		Container container = makeTitlePanel();
		add(container);
	}

	protected void initSpringBox() {
		String label = null == resourceBundle ? "" : resourceBundle.getString(SPRING_CONFIGURATION_LABEL_RESOURCE);
		JLabel jLabel = new JLabel(label);
		initSpringBox(jLabel);
	}

	protected void initSpringBox(JLabel label) {
		Box box = Box.createHorizontalBox();
		box.add(label);
		initSpringBox(box);
	}

	protected void initSpringBox(Box box) {
		box.add(springContextField);
		add(box);
	}

	@Override
	public TestElement createTestElement() {
		MartiniPreProcessor preProcessor = new MartiniPreProcessor();
		modifyTestElement(preProcessor);
		return preProcessor;
	}

	@Override
	public void modifyTestElement(TestElement preProcessor) {
		super.configureTestElement(preProcessor);
		String configuration = springContextField.getText();
		preProcessor.setProperty(PROPERTY_KEY_SPRING_CONFIGURATION, configuration);
	}

	@Override
	public void configure(TestElement el) {
		super.configure(el);
		String contextConfiguration = el.getPropertyAsString(PROPERTY_KEY_SPRING_CONFIGURATION);
		springContextField.setText(contextConfiguration);
	}
}
