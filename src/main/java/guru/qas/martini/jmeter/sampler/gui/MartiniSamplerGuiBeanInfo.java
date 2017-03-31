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

package guru.qas.martini.jmeter.sampler.gui;

import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.MartiniConstants.RESOURCE_BUNDLE_TITLE;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniSamplerGuiBeanInfo extends SimpleBeanInfo {

	protected static final Logger LOGGER = LoggingManager.getLoggerFor(MartiniSamplerGuiBeanInfo.class.getName());

	protected final ResourceBundle resourceBundle;

	public MartiniSamplerGuiBeanInfo() {
		String baseName = MartiniSamplerGui.class.getName();
		resourceBundle = ResourceBundle.getBundle(baseName);
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		BeanDescriptor descriptor = new BeanDescriptor(MartiniSamplerGui.class);
		String title = resourceBundle.getString(RESOURCE_BUNDLE_TITLE);
		descriptor.setDisplayName(title);

		Enumeration<String> keys = resourceBundle.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = getResource(key);
			descriptor.setValue(key, value);
		}
		return descriptor;
	}

	protected String getResource(ResourceBundle bundle, String key) {
		String value = bundle.getString(key).trim();
		checkState(!value.isEmpty(), "ResourceBundle missing value for key %s", RESOURCE_BUNDLE_TITLE);
		return value;
	}

	protected String getResource(String key) {
		String resource = null;
		try {
			resource = resourceBundle.getString(key);
		}
		catch (Exception e) {
			LOGGER.warn("unable to retrieve String value for ResourceBundle key %s; defaulting to null", e);
		}
		return resource;
	}

	@Override
	public Image getIcon(int iconKind) {
		String key = String.format("icon.%s", iconKind);
		String resource = getResource(key);
		return null == resource ? null : super.loadImage(resource);
	}
}
