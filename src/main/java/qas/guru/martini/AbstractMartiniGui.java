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

import org.apache.jmeter.gui.AbstractJMeterGuiComponent;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniGui extends AbstractJMeterGuiComponent {

	private static final long serialVersionUID = -8876922196511437500L;

	protected final Logger logger;
	protected final Class implementation;
	protected final IconManager iconManager;

	protected ResourceBundleManager bundleManagerRef;

	protected AbstractMartiniGui() {
		super(); // Careful, calls getStaticLabel().
		implementation = getClass();
		logger = LoggingManager.getLoggerFor(implementation.getName());
		iconManager = DefaultIconManager.getInstance();
		init();
	}

	protected void init() {
		iconManager.registerIcons(implementation);
	}

	@Override
	public String getStaticLabel() {
		String key = getLabelResource();
		return getStaticLabel(key);

	}

	protected String getStaticLabel(String key) {
		ResourceBundleManager bundleManager = getResourceBundleManager();
		String label = bundleManager.get(key);
		return null == label ? key : label;
	}

	protected ResourceBundleManager getResourceBundleManager() {
		if (null == bundleManagerRef) {
			bundleManagerRef = DefaultResourceBundleManager.getInstance();
		}
		return bundleManagerRef;
	}

	@Override
	public String getLabelResource() {
		return String.format("%s.title", getClass().getName());
	}
}
