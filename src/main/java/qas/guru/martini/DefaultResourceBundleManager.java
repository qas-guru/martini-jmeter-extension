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

import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class DefaultResourceBundleManager implements ResourceBundleManager {

	protected static final AtomicReference<DefaultResourceBundleManager> REF = new AtomicReference<>();
	protected static final String BASE_NAME = "qas.guru.martini.jmeter";

	protected final ResourceBundle bundle;
	protected final Logger logger;

	protected DefaultResourceBundleManager(ResourceBundle bundle, Logger logger) {
		this.bundle = bundle;
		this.logger = logger;
	}

	@Override
	public String get(String key) {
		String trimmed = checkNotNull(key, "null String").trim();
		checkArgument(!trimmed.isEmpty(), "empty String");
		return getString(trimmed);
	}

	protected String getString(String key) {
		String value = null;
		try {
			value = bundle.getString(key).trim();
			if (value.isEmpty()) {
				String message = String.format("value for key %s in ResourceBundle %s is blank", key, bundle);
				logger.warn(message);
			}
		}
		catch (Exception e) {
			String message = String.format("unable to obtain value for key %s in ResourceBundle %s", key, bundle);
			logger.warn(message, e);
		}
		return value;
	}

	public static DefaultResourceBundleManager getInstance() {
		DefaultResourceBundleManager instance;
		synchronized (REF) {
			instance = REF.get();
			if (null == instance) {
				ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME);
				Logger logger = LoggingManager.getLoggerFor(DefaultResourceBundleManager.class.getName());
				instance = new DefaultResourceBundleManager(bundle, logger);
				REF.set(instance);
			}
		}
		return instance;
	}
}
