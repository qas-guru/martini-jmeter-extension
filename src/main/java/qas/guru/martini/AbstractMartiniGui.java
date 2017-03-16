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

import java.net.URL;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.apache.jmeter.gui.AbstractJMeterGuiComponent;
import org.apache.jmeter.gui.GUIFactory;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.io.Resources;

import static javax.swing.JOptionPane.WARNING_MESSAGE;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniGui extends AbstractJMeterGuiComponent {

	private static final long serialVersionUID = -8876922196511437500L;

	protected static final String RESOURCE_BASE_NAME = "qas.guru.martini.jmeter";

	protected final Logger logger;
	protected final ResourceBundle resourceBundle;
	protected final String titleKey;

	protected AbstractMartiniGui(String titleKey) {
		super();
		this.logger = LoggingManager.getLoggerFor(getClass().getName());
		this.resourceBundle = getResourceBundle();
		this.titleKey = titleKey;
	}

	protected ResourceBundle getResourceBundle() {
		return getResourceBundle(RESOURCE_BASE_NAME);
	}

	protected ResourceBundle getResourceBundle(String baseName) {
		ResourceBundle bundle = null;
		try {
			bundle = ResourceBundle.getBundle(baseName);
		}
		catch (Exception e) {
			String message = String.format("unable to load resource bundle %s", baseName);
			logger.warn(message, e);
			JOptionPane.showMessageDialog(this, message + "; see log for details", "Warning", WARNING_MESSAGE);
		}
		return bundle;
	}

	protected void registerEnabledIcon(String resourceName) {
		ImageIcon icon = getImageIcon(resourceName);
		if (null != icon) {
			String key = getClass().getName();
			GUIFactory.registerIcon(key, icon);
		}
	}

	protected void registerDisabledIcon(String resourceName) {
		ImageIcon icon = getImageIcon(resourceName);
		if (null != icon) {
			String key = getClass().getName();
			GUIFactory.registerDisabledIcon(key, icon);
		}
	}

	protected ImageIcon getImageIcon(String resourceName) {
		ImageIcon icon = null;
		try {
			URL resource = Resources.getResource(resourceName);
			icon = new ImageIcon(resource);
		}
		catch (Exception e) {
			logger.warn("unable to load icon " + resourceName, e);
		}
		return icon;
	}

	protected abstract void initGui();

	@Override
	public String getLabelResource() {
		return titleKey;
	}

	@Override
	public String getStaticLabel() {
		return getResourceValue(titleKey);
	}

	protected String getResourceValue(String key) {
		String value = null;
		if (null != resourceBundle) {
			try {
				value = resourceBundle.getString(key);
			}
			catch (Exception e) {
				String baseName = resourceBundle.getBaseBundleName();
				String message = String.format("unable to find key %s in ResourceBundle %s", key, baseName);
				logger.warn(message, e);
			}
		}
		return null == value ? key : value;
	}
}
