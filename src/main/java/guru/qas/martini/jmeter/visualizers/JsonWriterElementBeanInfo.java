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

package guru.qas.martini.jmeter.visualizers;

import java.beans.BeanDescriptor;
import java.beans.SimpleBeanInfo;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JsonWriterElementBeanInfo extends SimpleBeanInfo {

	protected static final Logger LOGGER =
		LoggingManager.getLoggerFor(JsonWriterElementBeanInfo.class.getName());

	protected final ResourceBundle resourceBundle;

	public JsonWriterElementBeanInfo() {
		String baseName = JsonWriterElement.class.getName();
		resourceBundle = ResourceBundle.getBundle(baseName);
	}

	@Override
	public BeanDescriptor getBeanDescriptor() {
		BeanDescriptor descriptor = new BeanDescriptor(JsonWriterElement.class);

		Enumeration<String> keys = resourceBundle.getKeys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			String value = getResource(key);
			descriptor.setValue(key, value);
		}
		return descriptor;
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
}
