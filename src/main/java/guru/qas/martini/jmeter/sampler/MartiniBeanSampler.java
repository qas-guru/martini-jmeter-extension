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

package guru.qas.martini.jmeter.sampler;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;

import com.google.common.collect.ImmutableList;

import guru.qas.martini.jmeter.BeanHelper;
import guru.qas.martini.jmeter.DefaultBeanHelper;

@SuppressWarnings("WeakerAccess")
public class MartiniBeanSampler extends AbstractGenericSampler
	implements Serializable, Cloneable, TestStateListener, TestBean {

	private static final long serialVersionUID = -8693642618909458802L;

	// These must match field names exactly.
	protected static final String PROPERTY_BEAN_IMPLEMENTATION = "beanImplementation";
	protected static final String PROPERTY_BEAN_NAME = "beanName";
	protected static final String PROPERTY_BEAN_PROPERTIES = "beanProperties";

	// Serialized.
	protected String beanImplementation;
	protected String beanName;
	protected List<Argument> beanProperties;

	// Per-thread, but should only be referenced by startup thread.
	protected transient BeanHelper<BeanSampler> beanHelper;
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

	public MartiniBeanSampler() {
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
	protected BeanInfoSupport getBeanInfoSupport() throws Exception {
		return new MartiniBeanSamplerBeanInfo();
	}

	@Override
	public Object clone() {
		Object clone;
		if (started) {
			try {
				clone = beanHelper.getClone();
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
		beanHelper = DefaultBeanHelper.<BeanSampler>builder()
			.setHost(host)
			.setComponentName(getName())
			.setBeanInfoSupport(beanInfoSupport)
			.setBaseImplementation(BeanSampler.class)
			.setBeanImplementationProperty(beanImplementationProperty)
			.setBeanNameProperty(beanNameProperty)
			.setBeanProperties(arguments)
			.build();
		started = true;
	}

	@Override
	public SampleResult sample(Entry e) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void beginTearDown() {
		started = false;
		beanHelper = null;
	}
}
