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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.ImageIcon;

import org.apache.jmeter.gui.GUIFactory;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.io.Resources;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class DefaultIconManager implements IconManager {

	protected static final AtomicReference<DefaultIconManager> REF = new AtomicReference<>();

	protected final ResourceBundleManager bundle;
	protected final Logger logger;
	protected final Set<Class> initialized;

	protected DefaultIconManager(ResourceBundleManager bundle, Logger logger) {
		this.bundle = bundle;
		this.logger = logger;
		initialized = new HashSet<>();
	}

	@Override
	public void registerIcons(Class implementation) {
		checkNotNull(implementation, "null Class");
		synchronized (initialized) {
			if (!initialized.contains(implementation)) {
				registerIcon(implementation, Type.ENABLED);
				registerIcon(implementation, Type.DISABLED);
				initialized.add(implementation);
			}
		}
	}

	protected void registerIcon(Class implementation, Type type) {
		try {
			String key = type.getKey(implementation);
			String location = bundle.get(key);
			checkArgument(null != location && !location.isEmpty(), "no resource specified");
			registerIcon(implementation, type, location);
		}
		catch (Exception e) {
			String message = String.format("unable register %s icon for class %s", type, implementation);
			logger.warn(message, e);
		}
	}

	protected void registerIcon(Class implementation, Type type, String location) {
		URL resource = Resources.getResource(location);
		ImageIcon imageIcon = new ImageIcon(resource);
		type.register(implementation, imageIcon);
	}

	protected enum Type {
		ENABLED, DISABLED;

		protected static final String TEMPLATE = "%s.icon.%s";

		protected String getKey(Class implementation) {
			String className = implementation.getName();
			switch (this) {
				case ENABLED:
					return String.format(TEMPLATE, className, "enabled");
				case DISABLED:
					return String.format(TEMPLATE, className, "disabled");
				default:
					throw new UnsupportedOperationException("unrecognized Type: " + this);
			}
		}

		protected void register(Class implementation, ImageIcon icon) {
			String className = implementation.getName();
			switch (this) {
				case ENABLED:
					GUIFactory.registerIcon(className, icon);
					break;
				case DISABLED:
					GUIFactory.registerDisabledIcon(className, icon);
					break;
				default:
					throw new UnsupportedOperationException("unrecognized Type: " + this);
			}
		}
	}

	public static DefaultIconManager getInstance() {
		DefaultIconManager instance;
		synchronized (REF) {
			instance = REF.get();
			if (null == instance) {
				DefaultResourceBundleManager bundle = DefaultResourceBundleManager.getInstance();
				Logger logger = LoggingManager.getLoggerFor(DefaultIconManager.class.getName());
				instance = new DefaultIconManager(bundle, logger);
				REF.set(instance);
			}
		}
		return instance;
	}
}
