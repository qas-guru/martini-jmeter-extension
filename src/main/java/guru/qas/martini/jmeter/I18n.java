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
public class I18n {

	private static final Logger LOGGER = LoggerFactory.getLogger(I18n.class);

	protected I18n() {
	}

	public static String getMessage(Class implementation, String key, Object... arguments) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");

		String message = key;
		try {
			String template = getTemplate(implementation, key);
			if (null != template) {
				message = null == arguments ? template : String.format(template, arguments);
			}
		}
		catch (Exception e) {
			LOGGER.warn("unable to interpolate message for implementation {}, key {}", implementation, key);
		}
		return message;
	}

	protected static String getTemplate(Class implementation, String key) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");
		ResourceBundle bundle = getResourceBundle(implementation);
		return null == bundle ? key : getText(key, bundle);
	}

	private static ResourceBundle getResourceBundle(Class implementation) {
		String name = implementation.getCanonicalName();

		ResourceBundle bundle = null;
		try {
			checkNotNull(name, "Class has no canonical name: %s", implementation);
			String baseName = String.format("%sI18n", name);
			Locale locale = JMeterUtils.getLocale();
			bundle = ResourceBundle.getBundle(baseName, locale); // ResourceBundle performs caching.
		}
		catch (Exception e) {
			LOGGER.warn("unable to retrieve ResourceBundle for class: {}", implementation, e);
		}
		return bundle;
	}

	protected static String getText(String key, ResourceBundle bundle) {
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
