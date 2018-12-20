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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.DefaultTestBeanFactoryMessages.*;

@SuppressWarnings("WeakerAccess")
public class DefaultTestBeanFactory<T extends TestElement> implements TestBeanFactory<T> {

	protected final String componentName;
	protected final Class<? extends T> beanImplementation;
	protected final String beanName;
	protected final ImmutableList<Argument> properties;
	protected final String host;

	protected DefaultTestBeanFactory(
		String componentName,
		Class<? extends T> beanImplementation,
		@Nullable String beanName,
		ImmutableList<Argument> properties,
		@Nullable String host
	) {
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
		return new Builder<>();
	}

	public static class Builder<T extends TestElement> {

		protected String host;
		protected String componentName;
		protected BeanInfoSupport beanInfoSupport;
		protected Class<T> baseType;
		protected JMeterProperty baseTypeProperty;
		protected JMeterProperty nameProperty;
		protected ImmutableList<Argument> beanProperties;

		protected Builder() {
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

		public Builder<T> setBaseType(Class<T> i) {
			this.baseType = i;
			return this;
		}

		public Builder<T> setBaseTypeProperty(JMeterProperty p) {
			baseTypeProperty = p;
			return this;
		}

		public Builder<T> setNameProperty(JMeterProperty p) {
			nameProperty = p;
			return this;
		}

		public Builder<T> setBeanProperties(Collection<Argument> i) {
			this.beanProperties = null == i ? ImmutableList.of() : ImmutableList.copyOf(i);
			return this;
		}

		@SuppressWarnings("RedundantThrows")
		public DefaultTestBeanFactory<T> build() throws Exception {
			checkState(null != beanInfoSupport, "null BeanInfoSupport");
			checkState(null != baseType, "null Class");

			String beanName = getStringValue(nameProperty);
			String beanClass = getStringValue(baseTypeProperty);

			assertImplementationOrNameProvided(beanClass, beanName);

			Class<? extends T> implementation = null == beanClass ? null : getClass(beanClass);
			String definitionName = this.getBeanDefinitionName(implementation, beanName);
			BeanDefinition definition = getBeanDefinition(definitionName);
			assertValid(definitionName, definition);

			String displayName = getDisplayName();
			return new DefaultTestBeanFactory<>(displayName, implementation, beanName, beanProperties, host);
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
				String typeLabel = getDisplayName(baseTypeProperty);
				String nameLabel = getDisplayName(nameProperty);
				String message = Messages.getMessage(BEAN_TYPE_OR_NAME_REQUIRED, typeLabel, nameLabel);
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

		@SuppressWarnings("unchecked")
		protected Class<T> getClass(String implementation) {
			Class candidate = getCandidate(implementation);
			assertExpectedType(candidate);
			return (Class<T>) candidate;
		}

		protected Class getCandidate(String implementation) {
			try {
				Class<? extends Builder> myClass = this.getClass();
				ClassLoader classLoader = myClass.getClassLoader();
				return Class.forName(implementation, false, classLoader);
			}
			catch (ClassNotFoundException e) {
				String typeLabel = getDisplayName(baseTypeProperty);
				String message = Messages.getMessage(MISSING_IMPLEMENTATION, typeLabel, implementation);
				throw new IllegalArgumentException(message);
			}
		}

		protected void assertExpectedType(Class candidate) {
			if (!this.baseType.isAssignableFrom(candidate)) {
				String typeLabel = getDisplayName(baseTypeProperty);
				String actualClassName = candidate.getName();
				String expectedClassName = baseType.getName();
				String message = Messages.getMessage(INVALID_IMPLEMENTATION, typeLabel, actualClassName, expectedClassName);
				throw new IllegalArgumentException(message);
			}
		}

		protected String getBeanDefinitionName(@Nullable Class<? extends T> type, @Nullable String beanName) {
			String[] beanNames = getBeanNames(type);

			String match;
			if (0 == beanNames.length) {
				throw exceptionOnUndefinedBean(type);
			}
			else if (null != beanName && Lists.newArrayList(beanNames).contains(beanName)) {
				match = beanName;
			}
			else if (null != beanName) {
				throw exceptionOnUndefinedBean(beanName, type);
			}
			else if (beanNames.length > 1) {
				throw exceptionOnAmbiguousBeanDefinition(type);
			}
			else {
				match = beanNames[0];
			}
			return match;
		}

		protected String[] getBeanNames(@Nullable Class<? extends T> type) {
			ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
			return null == type ?
				springContext.getBeanNamesForType(baseType) :
				springContext.getBeanNamesForType(type);
		}

		@SuppressWarnings("Duplicates")
		protected IllegalStateException exceptionOnUndefinedBean(@Nullable Class<? extends T> type) {
			String message;
			if (null == type) {
				String className = baseType.getName();
				message = Messages.getMessage(MISSING_BEAN_DEFINITION_BY_BASE_IMPLEMENTATION, className);
			}
			else {
				String beanTypeLabel = this.getDisplayName(baseTypeProperty);
				String className = type.getName();
				message = Messages.getMessage(MISSING_BEAN_DEFINITION_BY_BASE_EXTENSION, beanTypeLabel, className);
			}
			throw new IllegalStateException(message);
		}

		protected IllegalStateException exceptionOnUndefinedBean(@Nonnull String beanName, @Nullable Class<? extends T> type) {
			String beanNameLabel = getDisplayName(nameProperty);
			String message;
			if (null == type) {
				String className = baseType.getName();
				message = Messages.getMessage(MISSING_BEAN_DEFINITION_BY_NAME_ONLY, beanNameLabel, beanName, className);
			}
			else {
				String beanTypeLabel = getDisplayName(baseTypeProperty);
				String className = type.getName();
				message = Messages.getMessage(MISSING_BEAN_DEFINITION_BY_NAME_AND_IMPLEMENTATION, beanNameLabel, beanName, beanTypeLabel, className);
			}
			throw new IllegalArgumentException(message);
		}

		@SuppressWarnings("Duplicates")
		protected IllegalStateException exceptionOnAmbiguousBeanDefinition(@Nullable Class<? extends T> type) {
			String message;
			if (null == type) {
				String className = baseType.getName();
				message = Messages.getMessage(MULTIPLE_BEAN_DEFINITIONS_BY_BASE_IMPLEMENTATION, className);
			}
			else {
				String label = this.getDisplayName(baseTypeProperty);
				String className = type.getName();
				message = Messages.getMessage(MULTIPLE_BEAN_DEFINITIONS_BY_BASE_EXTENSION, label, className);
			}
			throw new IllegalArgumentException(message);
		}

		protected BeanDefinition getBeanDefinition(String beanDefinitionName) {
			ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
			ConfigurableListableBeanFactory beanFactory = springContext.getBeanFactory();
			return beanFactory.getMergedBeanDefinition(beanDefinitionName);
		}

		protected void assertValid(String definitionName, BeanDefinition definition) {
			if (!definition.isPrototype()) {
				String className = definition.getBeanClassName();
				String message = Messages.getMessage(BEAN_DEFINITION_NOT_PROTOTYPE, definitionName, className);
				throw new IllegalArgumentException(message);
			}
		}
	}
}
