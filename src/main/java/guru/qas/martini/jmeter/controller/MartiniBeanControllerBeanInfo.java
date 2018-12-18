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

package guru.qas.martini.jmeter.controller;

import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.GenericTestBeanCustomizer;
import org.apache.jmeter.testbeans.gui.TableEditor;
import org.apache.jmeter.testbeans.gui.TypeEditor;
import org.apache.jmeter.util.JMeterUtils;

import guru.qas.martini.ResourceBundleMessageFunction;
import guru.qas.martini.jmeter.TagResourceBundle;

import static guru.qas.martini.jmeter.controller.MartiniBeanController.*;

@SuppressWarnings("WeakerAccess")
public class MartiniBeanControllerBeanInfo extends BeanInfoSupport {

	protected Function<String, String> messageFunction;

	public MartiniBeanControllerBeanInfo() throws IOException {
		super(MartiniBeanController.class);
		setUpMessageFunction();
		init();
	}

	protected void setUpMessageFunction() {
		BeanDescriptor descriptor = super.getBeanDescriptor();
		messageFunction = ResourceBundleMessageFunction.getInstance(descriptor);
	}

	@SuppressWarnings("Duplicates")
	protected void init() throws IOException {
		String label = messageFunction.apply("options.label");
		createPropertyGroup(label, new String[]{
			PROPERTY_BEAN_IMPLEMENTATION,
			PROPERTY_BEAN_NAME,
			PROPERTY_BEAN_PROPERTIES});
		setUpBeanImplementation();
		setUpBeanName();
		setUpBeanProperties();
	}

	protected void setUpBeanImplementation() throws IOException {
		PropertyDescriptor p = property(PROPERTY_BEAN_IMPLEMENTATION);
		p.setValue(NOT_UNDEFINED, Boolean.FALSE);

		List<String> tags = JMeterUtils.findClassesThatExtend(BeanController.class);
		p.setValue(GenericTestBeanCustomizer.TAGS, tags.toArray(new String[0]));
		p.setValue(GenericTestBeanCustomizer.NOT_EXPRESSION, Boolean.FALSE);

		BeanDescriptor beanDescriptor = super.getBeanDescriptor();
		Object o = beanDescriptor.getValue(GenericTestBeanCustomizer.RESOURCE_BUNDLE);
		ResourceBundle resourceBundle = ResourceBundle.class.cast(o);
		TagResourceBundle tagResourceBundle = new TagResourceBundle(resourceBundle, tags);

		p.setValue(GenericTestBeanCustomizer.RESOURCE_BUNDLE, tagResourceBundle);
		p.setValue(GenericTestBeanCustomizer.GUITYPE, TypeEditor.ComboStringEditor);
	}

	protected void setUpBeanName() {
		PropertyDescriptor p = property(PROPERTY_BEAN_NAME);
		p.setValue(NOT_UNDEFINED, Boolean.FALSE);
	}

	protected void setUpBeanProperties() {
		PropertyDescriptor p = property(PROPERTY_BEAN_PROPERTIES);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, new ArrayList<Argument>());
		p.setPropertyEditorClass(TableEditor.class);
		p.setValue(TableEditor.CLASSNAME, Argument.class.getName());
		p.setValue(TableEditor.OBJECT_PROPERTIES, new String[]{"name", "value", "description"});

		String nameLabel = getLabel("properties.name.label");
		String valueLabel = getLabel("properties.value.label");
		String descriptionLabel = getLabel("properties.description.label");
		p.setValue(TableEditor.HEADERS, new String[]{nameLabel, valueLabel, descriptionLabel});
	}

	protected String getLabel(String key) {
		return messageFunction.apply(key);
	}
}
