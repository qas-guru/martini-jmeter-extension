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

package guru.qas.martini.jmeter;

import java.beans.BeanDescriptor;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nullable;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import ch.qos.cal10n.IMessageConveyor;
import ch.qos.cal10n.MessageConveyor;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.DefaultTestBeanFactoryMessages.*;

@SuppressWarnings("WeakerAccess")
public class DefaultTestBeanFactory<T extends TestElement> implements TestBeanFactory<T> {

	protected final IMessageConveyor messageConveyor;
	protected final String componentName;
	protected final Class<? extends T> beanImplementation;
	protected final String beanName;
	protected final ImmutableList<Argument> properties;
	protected final String host;

	protected DefaultTestBeanFactory(
		IMessageConveyor messageConveyor,
		String componentName,
		Class<? extends T> beanImplementation,
		@Nullable String beanName,
		ImmutableList<Argument> properties,
		@Nullable String host
	) {
		this.messageConveyor = messageConveyor;
		this.componentName = componentName;
		this.beanImplementation = beanImplementation;
		this.beanName = beanName;
		this.properties = properties;
		this.host = host;
	}

	@Override
	public T getBean() {
		T delegate = getDelegate();
		setProperties(delegate);
		delegate.setName(componentName);
		delegate.setRunningVersion(true);
		notifyStarted(delegate);
		return delegate;
	}

	protected T getDelegate() {
		ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
		return null == beanName ?
			springContext.getBean(beanImplementation) : springContext.getBean(beanName, beanImplementation);
	}

	protected void setProperties(T element) {
		if (null != properties) {
			properties.stream()
				.map(this::getStringProperty)
				.forEach(element::setProperty);
		}
	}

	protected StringProperty getStringProperty(Argument argument) {
		String name = argument.getName();
		String value = argument.getValue();
		return new StringProperty(name, value);
	}

	protected void notifyStarted(T element) {
		if (TestStateListener.class.isInstance(element)) {
			TestStateListener cast = TestStateListener.class.cast(element);
			if (null == host) {
				cast.testStarted();
			}
			else {
				cast.testStarted(host);
			}
		}
	}

	public static <T extends TestElement> Builder<T> builder() {
		Locale locale = JMeterUtils.getLocale();
		MessageConveyor messageConveyor = new MessageConveyor(locale);
		return new Builder<T>(messageConveyor);
	}

	public static class Builder<T extends TestElement> {

		protected IMessageConveyor messageConveyor;

		protected String host;
		protected String componentName;
		protected BeanInfoSupport beanInfoSupport;
		protected Class<T> baseImplementation;
		protected JMeterProperty beanImplementationProperty;
		protected JMeterProperty beanNameProperty;
		protected ImmutableList<Argument> beanProperties;

		protected Builder(IMessageConveyor messageConveyor) {
			this.messageConveyor = messageConveyor;
		}

		public Builder<T> setHost(String s) {
			this.host = s;
			return this;
		}

		public Builder<T> setComponentName(String s) {
			this.componentName = s;
			return this;
		}

		public Builder<T> setBeanInfoSupport(BeanInfoSupport s) {
			this.beanInfoSupport = s;
			return this;
		}

		public Builder<T> setBaseImplementation(Class<T> i) {
			this.baseImplementation = i;
			return this;
		}

		public Builder<T> setBeanImplementationProperty(JMeterProperty p) {
			beanImplementationProperty = p;
			return this;
		}

		public Builder<T> setBeanNameProperty(JMeterProperty p) {
			beanNameProperty = p;
			return this;
		}

		public Builder<T> setBeanProperties(Collection<Argument> i) {
			this.beanProperties = null == i ? ImmutableList.of() : ImmutableList.copyOf(i);
			return this;
		}

		public DefaultTestBeanFactory<T> build() throws Exception {
			checkState(null != beanInfoSupport, "null BeanInfoSupport");
			checkState(null != baseImplementation, "null Class");

			String beanName = getStringValue(beanNameProperty);
			String beanClass = getStringValue(beanImplementationProperty);

			assertImplementationOrNameProvided(beanClass, beanName);

			Class<? extends T> implementation = null == beanClass ? null : getClass(beanClass);
			String definitionName = this.getBeanDefinitionName(implementation, beanName);
			BeanDefinition definition = getBeanDefinition(definitionName);
			assertValid(definitionName, definition);

			String displayName = getDisplayName();
			return new DefaultTestBeanFactory<>(messageConveyor, displayName, implementation, beanName, beanProperties, host);
		}

		protected String getDisplayName() {
			String displayName = componentName;
			if (null == displayName || displayName.trim().isEmpty()) {
				BeanDescriptor beanDescriptor = beanInfoSupport.getBeanDescriptor();
				displayName = beanDescriptor.getDisplayName();
			}
			return displayName;
		}

		protected String getStringValue(JMeterProperty property) {
			Object o = null == property ? null : property.getObjectValue();
			String s = null == o ? null : o.toString().trim();
			return null == s || s.isEmpty() ? null : s;
		}

		protected void assertImplementationOrNameProvided(String beanName, String implementation) {
			if (null == beanName && null == implementation) {
				String implementationDisplay = getDisplayName(beanImplementationProperty);
				String nameDisplay = getDisplayName(beanNameProperty);
				String message = messageConveyor.getMessage(
					NO_IMPLEMENTATION_OR_NAME_PROVIDED, implementationDisplay, nameDisplay);
				throw new IllegalStateException(message);
			}
		}

		protected String getDisplayName(JMeterProperty property) {
			String name = property.getName();
			PropertyDescriptor[] propertyDescriptors = beanInfoSupport.getPropertyDescriptors();
			PropertyDescriptor match = Arrays.stream(propertyDescriptors)
				.filter(descriptor -> name.equals(descriptor.getName()))
				.findFirst()
				.orElse(null);
			return null == match ? name : match.getDisplayName();
		}

		protected Class<T> getClass(String implementation) {
			try {
				Class<? extends Builder> myClass = this.getClass();
				ClassLoader classLoader = myClass.getClassLoader();
				Class candidate = Class.forName(implementation, false, classLoader);
				if (!baseImplementation.isAssignableFrom(candidate)) {
					String displayName = getDisplayName(beanImplementationProperty);
					String message = messageConveyor.getMessage(
						INVALID_IMPLEMENTATION, displayName, implementation, baseImplementation);
					throw new IllegalArgumentException(message);
				}
				//noinspection unchecked
				return (Class<T>) candidate;
			}
			catch (ClassNotFoundException e) {
				String displayName = getDisplayName(beanImplementationProperty);
				String message = messageConveyor.getMessage(MISSING_IMPLEMENTATION, displayName, implementation);
				throw new IllegalArgumentException(message);
			}
		}

		protected String getBeanDefinitionName(@Nullable Class<? extends T> implementation, @Nullable String beanName) {
			String[] beanNames = getBeanNames(implementation);

			String match;
			if (null != beanName && Lists.newArrayList(beanNames).contains(beanName)) {
				match = beanName;
			}
			else if (null != beanName) {
				String beanNameDisplay = this.getDisplayName(beanNameProperty);
				String message;
				if (null == implementation) {
					String className = baseImplementation.getName();
					message = messageConveyor.getMessage(
						MISSING_BEAN_DEFINITION_BY_NAME_ONLY, beanNameDisplay, beanName, className);
				}
				else {
					String implementationDisplay = this.getDisplayName(beanImplementationProperty);
					String className = implementation.getName();
					message = messageConveyor.getMessage(
						MISSING_BEAN_DEFINITION_BY_NAME_AND_IMPLEMENTATION,
						beanNameDisplay,
						beanName,
						implementationDisplay,
						className);
				}
				throw new IllegalArgumentException(message);
			}
			else if (beanNames.length > 1) {
				String message;
				if (null == implementation) {
					String className = baseImplementation.getName();
					message = messageConveyor.getMessage(MULTIPLE_BEAN_DEFINITIONS_BY_BASE_IMPLEMENTATION, className);
				}
				else {
					String display = this.getDisplayName(beanImplementationProperty);
					String className = implementation.getName();
					message = messageConveyor.getMessage(MULTIPLE_BEAN_DEFINITIONS_BY_BASE_EXTENSION, display, className);

				}
				throw new IllegalArgumentException(message);
			}
			else {
				match = beanNames[0];
			}

			return match;
		}

		protected String[] getBeanNames(@Nullable Class<? extends T> implementation) {
			ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();

			String[] beanNames = null == implementation ?
				springContext.getBeanNamesForType(baseImplementation) : springContext.getBeanNamesForType(implementation);
			if (0 == beanNames.length) {
				String message;
				if (null == implementation) {
					String name = baseImplementation.getName();
					message = messageConveyor.getMessage(MISSING_BEAN_DEFINITION_BY_BASE_IMPLEMENTATION, name);
				}
				else {
					String displayName = this.getDisplayName(beanImplementationProperty);
					String name = implementation.getName();
					message = messageConveyor.getMessage(MISSING_BEAN_DEFINITION_BY_BASE_EXTENSION, displayName, name);
				}
				throw new IllegalStateException(message);
			}
			return beanNames;
		}

		protected BeanDefinition getBeanDefinition(String beanDefinitionName) {
			ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
			ConfigurableListableBeanFactory beanFactory = springContext.getBeanFactory();
			return beanFactory.getMergedBeanDefinition(beanDefinitionName);
		}

		protected void assertValid(String definitionName, BeanDefinition definition) {
			if (!definition.isPrototype()) {
				String className = definition.getBeanClassName();
				String message = messageConveyor.getMessage(BEAN_DEFINITION_NOT_PROTOTYPE, definitionName, className);
				throw new IllegalArgumentException(message);
			}
		}
	}
}
