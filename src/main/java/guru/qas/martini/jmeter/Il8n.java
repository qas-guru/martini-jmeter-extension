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
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class Il8n extends CacheLoader<Class, ResourceBundle> {

	private static final Logger LOGGER = LoggerFactory.getLogger(Il8n.class);
	protected static final Il8n INSTANCE = new Il8n();

	private final Locale locale;
	private final LoadingCache<Class, ResourceBundle> cache;

	protected Il8n() {
		locale = JMeterUtils.getLocale();
		cache = CacheBuilder.newBuilder()
			.maximumSize(10)
			.expireAfterAccess(1, TimeUnit.MINUTES)
			.build(this);
	}

	@Override
	public ResourceBundle load(@Nonnull Class c) {
		String name = c.getCanonicalName();
		return ResourceBundle.getBundle(name, locale);
	}

	public static Il8n getInstance() {
		return INSTANCE;
	}

	public String getStaticLabel(JMeterGUIComponent component) {
		checkNotNull(component, "null JMeterGUIComponent");
		Class implementation = component.getClass();
		String key = component.getLabelResource();

		String label = key;
		try {
			ResourceBundle bundle = cache.get(implementation);
			label = bundle.getString(key).trim();
		}
		catch (Exception e) {
			LOGGER.warn("unable to retrieve label from ResourceBundle", e);
		}
		return label;
	}
}
