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

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

import guru.qas.martini.event.BeforeSuiteEvent;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.jmeter.SamplerContext;
import guru.qas.martini.jmeter.Variables;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
@Configurable
public class MartiniSuitePreProcessor extends AbstractPreProcessor
	implements Serializable, Cloneable, TestBean, ApplicationListener<BeforeSuiteEvent> {

	private static final long serialVersionUID = -3444643765535879540L;

	// Shared.
	protected transient MartiniSuitePreProcessorBean bean;
	protected transient AtomicReference<SuiteIdentifier> suiteIdentifierRef;

	@Autowired
	protected void set(MartiniSuitePreProcessorBean bean) {
		this.bean = bean;
	}

	public MartiniSuitePreProcessor() {
		super();
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() {
		return new MartiniSuitePreProcessorBeanInfo();
	}

	@Override
	protected void completeSetup() {
		suiteIdentifierRef = new AtomicReference<>(null);
		ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
		AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
		beanFactory.autowireBean(this);
		bean.publishBeforeSuite();
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniSuitePreProcessor clone = MartiniSuitePreProcessor.class.cast(o);
		clone.bean = bean;
		return clone;
	}

	@Override
	public void onApplicationEvent(@Nonnull BeforeSuiteEvent event) {
		checkNotNull(event, "null BeforeSuiteEvent");
		SuiteIdentifier suiteIdentifier = event.getPayload();
		if (suiteIdentifierRef.compareAndSet(null, suiteIdentifier)) {
			Variables.set(suiteIdentifier);
		}
	}

	@Override
	public void process() {
		SuiteIdentifier suiteIdentifier = suiteIdentifierRef.get();
		SamplerContext.set(suiteIdentifier);
	}

	@Override
	protected void beginTearDown() {
		bean.publishAfterSuite();
		bean = null;
		suiteIdentifierRef.set(null);
	}
}