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

package qas.guru.martini.jmeter.visualizers;

import java.awt.BorderLayout;
import java.awt.Container;
import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.io.File;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.jmeter.gui.UnsharedComponent;
import org.apache.jmeter.gui.util.FilePanel;
import org.apache.jmeter.reporters.AbstractListenerElement;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.visualizers.Visualizer;
import org.apache.jmeter.visualizers.gui.AbstractListenerGui;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings("WeakerAccess")
public class JsonDataWriter extends AbstractListenerGui
	implements Visualizer, ChangeListener, UnsharedComponent {

	private static final long serialVersionUID = 3185045200220461800L;

	protected Logger logger;
	protected FilePanel filePanel;
	protected JsonWriterElement listenerElement;

	protected BeanDescriptor beanDescriptorRef;

	public JsonDataWriter() {
		super();
		init();
	}

	private void init() { // WARNING: called from ctor so must not be overridden (i.e. must be private or final)
		initLogger();
		initFilePanel();
		setName(getStaticLabel());

		setLayout(new BorderLayout());
		setBorder(makeBorder());
		add(makeTitlePanel(), BorderLayout.NORTH);
	}

	protected void initLogger() {
		Class implementation = getClass();
		String category = implementation.getName();
		logger = LoggingManager.getLoggerFor(category);
	}

	protected void initFilePanel() {
		BeanDescriptor beanDescriptor = getBeanDescriptor();
		String key = "file.panel.title";
		Object value = beanDescriptor.getValue(key);
		String title = String.class.isInstance(value) ? String.class.cast(value) : key;
		filePanel = new FilePanel(title, true); // only directories
		filePanel.addChangeListener(this);
	}

	@Override
	public String getStaticLabel() {
		return getBeanDescriptor().getDisplayName();
	}

	protected BeanDescriptor getBeanDescriptor() {
		if (null == beanDescriptorRef) {
			try {
				BeanInfo beanInfo = Introspector.getBeanInfo(getClass());
				beanDescriptorRef = beanInfo.getBeanDescriptor();
			}
			catch (Exception e) {
				throw new RuntimeException("unable to load BeanDescriptor", e);
			}
		}
		return beanDescriptorRef;
	}

	@Override
	protected Container makeTitlePanel() {
		Container panel = super.makeTitlePanel();
		panel.add(filePanel);
		return panel;
	}

	@Override
	public String getLabelResource() {
		return "label.jsonDataWriter";
	}

	@Override
	public TestElement createTestElement() {
		if (listenerElement == null) {
			listenerElement = new JsonWriterElement();
		}
		modifyTestElement(listenerElement);
		return (TestElement) listenerElement.clone();
	}

	@Override
	public void modifyTestElement(TestElement c) {
		configureTestElement((AbstractListenerElement) c);
		JsonWriterElement writerElement = JsonWriterElement.class.cast(c);
		String filename = filePanel.getFilename();
		writerElement.setFilename(filename);
		listenerElement = writerElement;
	}

	@Override
	public void configure(TestElement el) {
		super.configure(el);
		JsonWriterElement writerElement = JsonWriterElement.class.cast(el);
		String filename = writerElement.getFilename();
		filePanel.setFilename(filename);
	}

	/**
	 * This provides a convenience for extenders when they implement the
	 * {@link org.apache.jmeter.gui.JMeterGUIComponent#createTestElement()}
	 * method. This method will set the name, gui class, and test class for the
	 * created Test Element. It should be called by every extending class when
	 * creating Test Elements, as that will best assure consistent behavior.
	 *
	 * @param mc the TestElement being created.
	 */
	protected void configureTestElement(AbstractListenerElement mc) {
		// TODO: Should the method signature of this method be changed to
		// match the super-implementation (using a TestElement parameter
		// instead of AbstractListenerElement)? This would require an
		// instanceof check before adding the listener (below), but would
		// also make the behavior a bit more obvious for sub-classes -- the
		// Java rules dealing with this situation aren't always intuitive,
		// and a subclass may think it is calling this version of the method
		// when it is really calling the superclass version instead.
		super.configureTestElement(mc);
		mc.setListener(this);
	}

	@Override
	public void clearGui() {
		super.clearGui();
		filePanel.clearGui();
		String property = System.getProperty("user.dir");
		File file = new File(property);
		String path = file.getAbsolutePath();
		filePanel.setFilename(path);
	}

	@Override
	public void add(SampleResult sample) {
		System.out.println("breakpoint");
	}

	@Override
	public boolean isStats() {
		return false;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		if (filePanel.equals(source)) {
			String filename = filePanel.getFilename();
			if (null != listenerElement) {
				listenerElement.setFilename(filename);
			}
		}
	}
}
