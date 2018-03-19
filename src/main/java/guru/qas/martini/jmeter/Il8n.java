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

package guru.qas.martini.jmeter;

import java.util.Locale;

import java.util.ResourceBundle;

import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class Il8n {

	private static final Logger LOGGER = LoggerFactory.getLogger(Il8n.class);
	protected static final Il8n INSTANCE = new Il8n();

	private final Locale locale;

	protected Il8n() {
		locale = JMeterUtils.getLocale();
	}

	public static Il8n getInstance() {
		return INSTANCE;
	}

	public String getMessage(Class implementation, String key, Object... arguments) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");
		String template = getTemplate(implementation, key);
		return null == template ? key : null == arguments ? template : String.format(template, arguments);
	}

	protected String getTemplate(Class implementation, String key) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");
		ResourceBundle bundle = getResourceBundle(implementation);
		return null == bundle ? key : getText(key, bundle);
	}

	private ResourceBundle getResourceBundle(Class implementation) {
		String name = implementation.getCanonicalName();

		ResourceBundle bundle = null;
		try {
			checkNotNull(name, "Class has no canonical name: %s", implementation);
			String baseName = String.format("%sIl8n", name);
			bundle = ResourceBundle.getBundle(baseName, locale); // ResourceBundle performs caching.
		}
		catch (Exception e) {
			LOGGER.warn("unable to retrieve ResourceBundle for class: {}", implementation, e);
		}
		return bundle;
	}

	protected String getText(String key, ResourceBundle bundle) {
		String label = null;
		try {
			label = bundle.getString(key);
		}
		catch (Exception e) {
			LOGGER.warn("unable to find value by key {} in ResourceBundle {}", key, bundle, e);
		}
		return null == label ? key : label;
	}
}
