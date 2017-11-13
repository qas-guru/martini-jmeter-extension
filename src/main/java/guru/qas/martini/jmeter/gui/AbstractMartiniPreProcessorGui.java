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

package guru.qas.martini.jmeter.gui;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.processor.gui.AbstractPreProcessorGui;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;

abstract class AbstractMartiniPreProcessorGui extends AbstractPreProcessorGui {

	private final String labelResource;
	private final String staticLabel;
	private final String comment;
	private final Class<? extends TestElement> implementation;

	AbstractMartiniPreProcessorGui(
		String labelResource,
		String staticLabel,
		String comment,
		Class<? extends TestElement> implementation
	) {
		this.labelResource = labelResource;
		this.staticLabel = staticLabel;
		this.comment = comment;
		this.implementation = implementation;
	}

	@Override
	public String getLabelResource() {
		return labelResource;
	}

	@Override
	public String getStaticLabel() {
		return staticLabel;
	}

	TestElement createTestElement(Arguments defaults) {
		TestElement testElement = getNewInstance();
		super.configureTestElement(testElement);
		testElement.setName(staticLabel);
		testElement.setComment(comment);
		setDefaults(testElement, defaults);
		return testElement;
	}

	private TestElement getNewInstance() {
		try {
			return implementation.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException("unable to instantiate " + implementation, e);
		}
	}

	private void setDefaults(TestElement element, Arguments defaults) {
		for (JMeterProperty property : defaults) {
			Object o = property.getObjectValue();
			Argument argument = Argument.class.cast(o);
			String name = argument.getName();
			String value = argument.getValue();
			element.setProperty(name, value);
		}
	}

	@Override
	public void modifyTestElement(TestElement testElement) {
		String name = super.getName();
		testElement.setName(name);

		String comment = super.getComment();
		testElement.setComment(comment);
	}
}
