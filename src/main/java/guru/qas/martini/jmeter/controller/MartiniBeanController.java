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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.common.collect.ImmutableList;

import guru.qas.martini.jmeter.TestBeanFactory;
import guru.qas.martini.jmeter.DefaultTestBeanFactory;

@SuppressWarnings("WeakerAccess")
@Configurable
public class MartiniBeanController extends AbstractGenericController
	implements Serializable, Cloneable, TestBean, TestStateListener {

	private static final long serialVersionUID = -4467996371415767533L;

	// These must match field names exactly.
	protected static final String PROPERTY_BEAN_IMPLEMENTATION = "beanImplementation";
	protected static final String PROPERTY_BEAN_NAME = "beanName";
	protected static final String PROPERTY_BEAN_PROPERTIES = "beanProperties";

	// Serialized.
	protected String beanImplementation;
	protected String beanName;
	protected List<Argument> beanProperties;

	// Per-thread, but should only be referenced by startup thread.
	protected transient TestBeanFactory<BeanController> testBeanFactory;
	protected transient boolean started;

	@SuppressWarnings("unused")
	public String getBeanImplementation() {
		return beanImplementation;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanImplementation(String beanImplementation) {
		this.beanImplementation = beanImplementation;
	}

	@SuppressWarnings("unused")
	public String getBeanName() {
		return beanName;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public List<Argument> getBeanProperties() {
		return beanProperties;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanProperties(List<Argument> beanProperties) {
		this.beanProperties = beanProperties;
	}

	public MartiniBeanController() {
		super();
		init();
	}

	public Object readResolve() {
		init();
		return this;
	}

	protected void init() {
		beanProperties = new ArrayList<>();
		started = false;
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() throws IOException {
		return new MartiniBeanControllerBeanInfo();
	}

	@SuppressWarnings("Duplicates")
	@Override
	public Object clone() {
		Object clone;
		if (started) {
			try {
				clone = testBeanFactory.getBean();
			}
			catch (Exception e) {
				throw new AssertionError(e);
			}
		}
		else {
			clone = super.clone();
		}
		return clone;
	}

	@Override
	protected void completeSetup() throws Exception {
		JMeterProperty beanImplementationProperty = super.getProperty(PROPERTY_BEAN_IMPLEMENTATION);
		JMeterProperty beanNameProperty = super.getProperty(PROPERTY_BEAN_NAME);
		ImmutableList<Argument> arguments = ImmutableList.copyOf(getBeanProperties());
		testBeanFactory = DefaultTestBeanFactory.<BeanController>builder()
			.setHost(host)
			.setComponentName(getName())
			.setBeanInfoSupport(beanInfoSupport)
			.setBaseType(BeanController.class)
			.setBaseTypeProperty(beanImplementationProperty)
			.setNameProperty(beanNameProperty)
			.setBeanProperties(arguments)
			.build();
		started = true;
	}

	@Override
	protected void beginTearDown() {
		started = false;
	}
}