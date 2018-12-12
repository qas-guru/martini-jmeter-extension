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

package guru.qas.martini.jmeter.preprocessor;

import java.beans.BeanDescriptor;

import java.util.function.Function;

import org.apache.jmeter.testbeans.BeanInfoSupport;

import guru.qas.martini.ResourceBundleMessageFunction;

@SuppressWarnings({"unused", "WeakerAccess"}) // Used by bean introspection.
public class MartiniSuitePreProcessorBeanInfo extends BeanInfoSupport {

	protected Function<String, String> messageFunction;

	public MartiniSuitePreProcessorBeanInfo() {
		super(MartiniSuitePreProcessor.class);
		init();
	}

	protected void init() {
		setUpIcon();
		setUpMessageFunction();
		setUpProperties();
	}

	protected void setUpIcon() {
		//super.setIcon("spring_icon_16x16.png");
	}

	protected void setUpMessageFunction() {
		BeanDescriptor descriptor = super.getBeanDescriptor();
		messageFunction = ResourceBundleMessageFunction.getInstance(descriptor);
	}

	protected void setUpProperties() {
	}

	protected String getLabel(String key) {
		return messageFunction.apply(key);
	}
}
