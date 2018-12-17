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

import javax.annotation.Nonnull;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.StringProperty;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import guru.qas.martini.jmeter.Variables;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.controller.MartiniBeanControllerMessages.*;

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

	// Per-thread.
	protected transient boolean started;

	public String getBeanImplementation() {
		return beanImplementation;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanImplementation(String s) {
		String trimmed = null == s ? "" : s.trim();
		this.beanImplementation = trimmed.isEmpty() ? null : trimmed;
	}

	public String getBeanName() {
		return beanName;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Nonnull
	public List<Argument> getBeanProperties() {
		return beanProperties;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setBeanProperties(List<Argument> beanProperties) {
		this.beanProperties.clear();
		if (null != beanProperties) {
			this.beanProperties.addAll(beanProperties);
		}
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
		started = false;
		beanProperties = new ArrayList<>();
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		Object clone;
		if (started) {
			try {
				BeanController delegate = getDelegate();
				setProperties(delegate);
				delegate.setRunningVersion(true);
				notifyStarted(delegate);
				clone = delegate;
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

	protected BeanController getDelegate() {
		Class<? extends BeanController> implementation =
			isBeanImplementationProvided() ? this.getImplementation() : BeanController.class;
		ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();

		return isBeanNameProvided() ?
			springContext.getBean(beanName, implementation) :
			springContext.getBean(implementation);
	}

	protected void setProperties(BeanController delegate) {
		getBeanProperties().stream()
			.map(argument -> {
				String name = argument.getName();
				String value = argument.getValue();
				return new StringProperty(name, value);
			}).forEach(delegate::setProperty);
	}

	protected void notifyStarted(BeanController delegate) {
		if (TestStateListener.class.isInstance(delegate)) {
			TestStateListener asListener = TestStateListener.class.cast(delegate);
			if (null == host) {
				asListener.testStarted();
			}
			else {
				asListener.testStarted(host);
			}
		}
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() throws IOException {
		return new MartiniBeanControllerBeanInfo();
	}

	@Override
	protected void completeSetup() {
		started = true;
		assertImplementationOrNameProvided();
		assertValidImplementation();
		assertValidBeanDefinition();
	}

	protected void assertImplementationOrNameProvided() {
		if (!isBeanImplementationProvided() && !isBeanNameProvided()) {
			String implementationLabel = messageFunction.apply(PROPERTY_BEAN_IMPLEMENTATION + ".displayName");
			String nameLabel = getNameLabel();
			String message = messageConveyor.getMessage(
				NO_IMPLEMENTATION_OR_NAME_PROVIDED, implementationLabel, nameLabel);
			throw new IllegalArgumentException(message);
		}
	}

	protected boolean isBeanImplementationProvided() {
		String implementation = getBeanImplementation();
		return null != implementation && !implementation.trim().isEmpty();
	}

	protected boolean isBeanNameProvided() {
		String name = getBeanName();
		return null != name && !name.trim().isEmpty();
	}

	protected String getNameLabel() {
		return messageFunction.apply(PROPERTY_BEAN_NAME + ".displayName");
	}

	protected void assertValidImplementation() {
		if (isBeanImplementationProvided()) {
			Class implementation = getImplementation();
			assertValidImplementation(implementation);
		}
	}

	protected Class getImplementation() {
		String type = getBeanImplementation().trim();
		return getImplementation(type, MISSING_IMPLEMENTATION, type);
	}

	protected Class<?> getImplementation(String className, MartiniBeanControllerMessages key, Object... args) {
		String type = className.trim();
		ClassLoader classLoader = getClass().getClassLoader();

		try {
			return Class.forName(type, false, classLoader);
		}
		catch (ClassNotFoundException e) {
			String message = messageConveyor.getMessage(key, args);
			throw new IllegalArgumentException(message, e);
		}
	}

	protected void assertValidImplementation(Class c) {
		checkState(BeanController.class.isAssignableFrom(c),
			messageConveyor.getMessage(INVALID_IMPLEMENTATION, BeanController.class.getName(), c.getName()));
	}

	protected void assertValidBeanDefinition() {
		ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();

		if (isBeanNameProvided()) {
			String beanName = this.getBeanName().trim();
			checkArgument(springContext.containsBean(beanName) || springContext.containsBeanDefinition(beanName),
				messageConveyor.getMessage(NO_DEFINITION_FOUND_BY_NAME, beanName));
			checkArgument(!springContext.isSingleton(beanName),
				messageConveyor.getMessage(SINGLETON_BEAN_NAME, beanName));

			if (isBeanImplementationProvided()) {
				ConfigurableListableBeanFactory beanFactory = springContext.getBeanFactory();
				BeanDefinition definition = beanFactory.getMergedBeanDefinition(beanName);
				String actualImplementation = definition.getBeanClassName();
				Class<?> beanClass = getImplementation(
					actualImplementation, INVALID_BEAN_DEFINITION, beanName, actualImplementation);

				Class<?> expectedImplementation = getImplementation();
				if (!expectedImplementation.isAssignableFrom(beanClass)) {
					String message = messageConveyor.getMessage(
						IMPLEMENTATION_MISMATCH, beanName, expectedImplementation, actualImplementation);
					throw new IllegalArgumentException(message);
				}
			}
		}
		else {
			Class<?> type = getImplementation();
			String[] beanNamesForType = springContext.getBeanNamesForType(type, true, true);
			int candidateCount = beanNamesForType.length;
			checkArgument(0 < candidateCount, messageConveyor.getMessage(IMPLEMENTATION_NOT_DEFINED, type));
			checkArgument(candidateCount < 2, messageConveyor.getMessage(MULTIPLE_IMPLEMENTATIONS_FOUND, type));
			String beanName = beanNamesForType[0];
			checkArgument(!springContext.isSingleton(beanName),
				messageConveyor.getMessage(SINGLETON_BEAN_IMPLEMENTATION, type));
		}
	}

	@Override
	protected void beginTearDown() {
		started = false;
	}
}