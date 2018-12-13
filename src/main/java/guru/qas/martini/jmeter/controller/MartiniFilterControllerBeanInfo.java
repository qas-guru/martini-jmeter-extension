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
import java.util.function.Function;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.GenericTestBeanCustomizer;
import org.apache.jmeter.testbeans.gui.LongPropertyEditor;
import org.apache.jmeter.testbeans.gui.TextAreaEditor;

import guru.qas.martini.ResourceBundleMessageFunction;

import static guru.qas.martini.jmeter.controller.MartiniFilterController.*;

@SuppressWarnings("WeakerAccess")
public class MartiniFilterControllerBeanInfo extends BeanInfoSupport {

	protected Function<String, String> messageFunction;

	public MartiniFilterControllerBeanInfo() {
		super(MartiniFilterController.class);
		init();
	}

	protected void init() {
		setUpMessageFunction();
		setUpProperties();
	}

	protected void setUpMessageFunction() {
		BeanDescriptor descriptor = super.getBeanDescriptor();
		messageFunction = ResourceBundleMessageFunction.getInstance(descriptor);
	}

	protected void setUpProperties() {
		String label = messageFunction.apply("options.label");
		createPropertyGroup(label, new String[]{
			PROPERTY_SPEL_FILTER,
			PROPERTY_NO_MARTINI_FOUND_FATAL,
			PROPERTY_UNIMPLEMENTED_STEPS_FATAL,
			PROPERTY_SHUFFLE,
			PROPERTY_RANDOM_SEED});

		setNoMartinisFoundFatal();
		setUnimplementedStepsFatalDescriptor();
		setSpelFilterDescriptor();
		setShuffleDescriptor();
		setRandomSeed();
	}

	protected void setNoMartinisFoundFatal() {
		PropertyDescriptor p = property(PROPERTY_NO_MARTINI_FOUND_FATAL);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.TRUE);
	}

	protected void setUnimplementedStepsFatalDescriptor() {
		PropertyDescriptor p = property(PROPERTY_UNIMPLEMENTED_STEPS_FATAL);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.TRUE);
	}

	protected void setSpelFilterDescriptor() {
		PropertyDescriptor p = property(PROPERTY_SPEL_FILTER);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, "");
		p.setPropertyEditorClass(TextAreaEditor.class);
		p.setValue(GenericTestBeanCustomizer.TEXT_LANGUAGE, "text");
	}

	protected void setShuffleDescriptor() {
		PropertyDescriptor p = property(PROPERTY_SHUFFLE);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, Boolean.FALSE);
	}

	protected void setRandomSeed() {
		PropertyDescriptor p = property(PROPERTY_RANDOM_SEED);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, 0L);
		p.setPropertyEditorClass(LongPropertyEditor.class);
	}
}
