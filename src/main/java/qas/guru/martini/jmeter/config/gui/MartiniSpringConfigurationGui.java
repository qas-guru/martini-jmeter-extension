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

package qas.guru.martini.jmeter.config.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.gui.util.MenuFactory;
import org.apache.jmeter.testelement.TestElement;

import qas.guru.martini.AbstractMartiniGui;
import qas.guru.martini.jmeter.config.MartiniSpringConfiguration;

@SuppressWarnings("WeakerAccess")
public class MartiniSpringConfigurationGui extends AbstractMartiniGui implements ActionListener {

	private static final long serialVersionUID = -4852200848794934491L;

	// TODO: replace this mess by distributing as a Plugin for use by PluginManager.
	protected static final AtomicBoolean ICONS_REGISTERED = new AtomicBoolean(false);
	protected static final String ICON_ENABLED = "icons/spring-logo.png";
	protected static final String ICON_DISABLED = "icons/spring-logo-disabled.png";

	protected static final Arguments DEFAULT_ARGUMENTS = new Arguments();

	static {
		DEFAULT_ARGUMENTS.addArgument("spring.profiles.active", "default", null, "active Spring profiles");
	}

	protected static final String DEFAULT_CONFIG_LOCATION = "applicationContext.xml";

	protected static final String TITLE_KEY = "martini_config_title";
	protected static final String LOCATION_LABEL_KEY = "martini_controller_spring_context_label";
	protected static final String ARGUMENTS_LABEL_KEY = "martini_controller_runtime_arguments_label";

	protected final JTextField contextLocationField;
	protected final ArgumentsPanel argumentsPanel;

	public MartiniSpringConfigurationGui() {
		this(TITLE_KEY);
	}

	public MartiniSpringConfigurationGui(String titleKey) {
		super(titleKey);
		contextLocationField = new JTextField(6);
		//contextLocationField.setText(DEFAULT_CONFIG_LOCATION);

		String label = super.getResourceValue(ARGUMENTS_LABEL_KEY);
		argumentsPanel = new ArgumentsPanel(label);

//		Object clone = DEFAULT_ARGUMENTS.clone();
//		Arguments arguments = Arguments.class.cast(clone);
//		argumentsPanel.configure(arguments);

		initGui();
	}

	@Override
	protected void initGui() {
		initIcons();

		this.setLayout(new BorderLayout(0, 5));
		this.setBorder(this.makeBorder());
		this.add(this.makeTitlePanel(), "North");

		JPanel panel = new JPanel(new BorderLayout(0, 5));
		Box box = getContextLocationBox();
		panel.add(box, "North");

		panel.add(argumentsPanel, "Center");
		this.add(panel, "Center");
	}

	// TODO: replace this mess by distributing as a Plugin for use by PluginManager.
	protected void initIcons() {
		synchronized (ICONS_REGISTERED) {
			if (!ICONS_REGISTERED.get()) {
				ICONS_REGISTERED.set(true);
				registerEnabledIcon(ICON_ENABLED);
				registerDisabledIcon(ICON_DISABLED);
			}
		}
	}

	protected Box getContextLocationBox() {
		String label = getResourceValue(LOCATION_LABEL_KEY);
		JLabel jLabel = new JLabel(label);
		return getContextLocationBox(jLabel);
	}

	protected Box getContextLocationBox(JLabel label) {
		Box box = Box.createHorizontalBox();
		box.add(label);
		box.add(contextLocationField);
		return box;
	}

	@Override
	public JPopupMenu createPopupMenu() {
		return MenuFactory.getDefaultConfigElementMenu();
	}

	@Override
	public Collection<String> getMenuCategories() {
		return Collections.singleton(MenuFactory.CONFIG_ELEMENTS);
	}

	@Override
	public void configure(TestElement element) {
		super.configure(element);
		MartiniSpringConfiguration config = MartiniSpringConfiguration.class.cast(element);
		String contextLocation = config.getConfigLocation();
		contextLocationField.setText(contextLocation);
		Arguments arguments = config.getArguments();
		argumentsPanel.configure(arguments);
	}

	@Override
	public TestElement createTestElement() {
		MartiniSpringConfiguration config = new MartiniSpringConfiguration();
		modifyTestElement(config);
		return config;
	}

	@Override
	public void modifyTestElement(TestElement element) {
		configureTestElement(element);

		MartiniSpringConfiguration configuration = MartiniSpringConfiguration.class.cast(element);
		TestElement testElement = argumentsPanel.createTestElement();
		Arguments arguments = Arguments.class.cast(testElement);
		configuration.setArguments(arguments);

		String location = contextLocationField.getText();
		configuration.setConfigLocation(location);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		argumentsPanel.clearGui();

		contextLocationField.setText(DEFAULT_CONFIG_LOCATION);

		Object clone = DEFAULT_ARGUMENTS.clone();
		Arguments arguments = Arguments.class.cast(clone);
		argumentsPanel.configure(arguments);
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		System.out.println("breakpoint");
	}


	/*
		public void actionPerformed(ActionEvent evt) {
	        if(evt.getSource() == this.classnameCombo) {
	            String className = ((String)this.classnameCombo.getSelectedItem()).trim();

	            try {
	                JavaSamplerClient e = (JavaSamplerClient)Class.forName(className, true, Thread.currentThread().getContextClassLoader()).newInstance();
	                Arguments currArgs = new Arguments();
	                this.argsPanel.modifyTestElement(currArgs);
	                Map currArgsMap = currArgs.getArgumentsAsMap();
	                Arguments newArgs = new Arguments();
	                Arguments testParams = null;

	                try {
	                    testParams = e.getDefaultParameters();
	                } catch (AbstractMethodError var14) {
	                    log.warn("JavaSamplerClient doesn\'t implement getDefaultParameters.  Default parameters won\'t be shown.  Please update your client class: " + className);
	                }

	                String name;
	                String value;
	                if(testParams != null) {
	                    for(Iterator i$ = testParams.getArguments().iterator(); i$.hasNext(); newArgs.addArgument(name, value)) {
	                        JMeterProperty jMeterProperty = (JMeterProperty)i$.next();
	                        Argument arg = (Argument)jMeterProperty.getObjectValue();
	                        name = arg.getName();
	                        value = arg.getValue();
	                        if(currArgsMap.containsKey(name)) {
	                            String newVal = (String)currArgsMap.get(name);
	                            if(newVal != null && newVal.length() > 0) {
	                                value = newVal;
	                            }
	                        }
	                    }
	                }

	                this.argsPanel.configure(newArgs);
	            } catch (Exception var15) {
	                log.error("Error getting argument list for " + className, var15);
	            }
	        }

	    }
	    */
}
