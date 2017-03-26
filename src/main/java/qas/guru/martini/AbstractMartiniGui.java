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
import java.awt.Image;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import org.apache.jmeter.gui.AbstractJMeterGuiComponent;
import org.apache.jmeter.gui.GUIFactory;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniGui extends AbstractJMeterGuiComponent {

	private static final long serialVersionUID = -8876922196511437500L;

	protected Logger logger;

	protected AbstractMartiniGui() throws Exception {
		super(); // Careful, calls getStaticLabel().
		init();
	}

	protected void init() throws Exception {
		initLogging();
		initIcon();
	}

	protected void initLogging() {
		Class<? extends AbstractMartiniGui> implementation = getClass();
		String category = implementation.getName();
		logger = LoggingManager.getLoggerFor(category);
	}

	protected String getDescriptorValue(String key) {
		BeanInfo beanInfo = getBeanInfo();
		BeanDescriptor descriptor = beanInfo.getBeanDescriptor();
		Object o = null == descriptor ? null : descriptor.getValue(key);
		return String.class.isInstance(o) ? String.class.cast(o) : getPlaceholder(key);
	}

	protected BeanDescriptor getDescriptor() {
		BeanInfo beanInfo = getBeanInfo();
		BeanDescriptor descriptor = beanInfo.getBeanDescriptor();
		return checkNotNull(descriptor, "BeanInfo returned null BeanDescriptor for class " + getClass());
	}

	protected BeanInfo getBeanInfo() {
		Class<? extends AbstractMartiniGui> implementation = getClass();
		try {
			return Introspector.getBeanInfo(implementation);
		}
		catch (IntrospectionException e) {
			throw new RuntimeException("unable to obtain BeanInfo for class " + implementation, e);
		}
	}

	protected String getPlaceholder(String key) {
		BeanDescriptor descriptor = getDescriptor();
		String name = descriptor.getName();
		String undefined = String.format("%s.%s", name, key);
		String message = String.format("BeanDescriptor missing property %s, returning %s", key, undefined);
		logger.warn(message);
		return undefined;
	}

	protected void initIcon() {
		BeanInfo beanInfo = getBeanInfo();
		Image icon = beanInfo.getIcon(BeanInfo.ICON_COLOR_16x16);
		if (null != icon) {
			ImageIcon imageIcon = new ImageIcon(icon);
			Class<? extends AbstractMartiniGui> implementation = getClass();
			String className = implementation.getName();
			GUIFactory.registerIcon(className, imageIcon);
		}
	}

	@Override
	public String getStaticLabel() {
		try {
			BeanInfo beanInfo = Introspector.getBeanInfo(getClass());
			BeanDescriptor descriptor = beanInfo.getBeanDescriptor();
			return descriptor.getDisplayName();
		}
		catch (IntrospectionException e) {
			throw new RuntimeException("unable to obtain BeanDescriptor", e);
		}
	}

	@Override
	public String getLabelResource() {
		throw new UnsupportedOperationException();
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
}
