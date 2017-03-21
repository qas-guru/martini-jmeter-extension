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

package qas.guru.martini;

import java.awt.BorderLayout;

import javax.swing.JPanel;

import org.apache.jmeter.gui.AbstractJMeterGuiComponent;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniGui extends AbstractJMeterGuiComponent {

	private static final long serialVersionUID = -8876922196511437500L;

	protected final IconManager iconManager;

	protected AbstractMartiniGui() {
		super(); // Careful, calls getStaticLabel().
		iconManager = DefaultIconManager.getInstance();
		iconManager.registerIcons(getClass());
	}

	@Override
	public String getStaticLabel() {
		String key = getLabelResource();
		return getResource(key, key);
	}

	@Override
	public String getLabelResource() {
		return getImplementationKey("%s.title");
	}

	protected String getImplementationKey(String template) {
		return String.format(template, getClass().getName());
	}

	protected void initGui() {
		initTitlePanel();
		JPanel mainPanel = new JPanel(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
	}

	protected void initTitlePanel() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);
	}

	protected String getImplementationResource(String template) {
		String key = getImplementationKey(template);
		return getResource(key, key);
	}

	protected String getResource(String key, String defaultValue) {
		ResourceBundleManager resourceManager = getResourceBundleManager();
		String value = resourceManager.get(key);
		if (null == value) {
			String message = String.format("unable to find value for key %s; returning default %s", key, defaultValue);
			Logger logger = LoggingManager.getLoggerFor(getClass().getName());
			logger.warn(message);
		}
		return null == value ? defaultValue : value;
	}

	protected ResourceBundleManager getResourceBundleManager() {
		return DefaultResourceBundleManager.getInstance();
	}
}
