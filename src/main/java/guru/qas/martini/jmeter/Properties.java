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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ResourceBundleMessageSource;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class Properties {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Properties.class);
	protected static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource();

	static {
		MESSAGE_SOURCE.setBasename("PropertiesBundle");
	}

	public static Optional<String> getDisplayName(TestElement element, String property) {
		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null String");

		Optional<PropertyDescriptor> match = getPropertyDescriptor(element, property);
		return match.isPresent() ? getDisplayName(match.get()) : Optional.empty();
	}

	private static Optional<String> getDisplayName(PropertyDescriptor d) {
		String displayName = d.getDisplayName();
		String trimmed = null == displayName ? "" : displayName.trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}

	@SuppressWarnings("unused")
	public static void assertPopulatedArgumentList(TestElement element, String property) {
		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null String");

		JMeterProperty jmeterProperty = element.getProperty(property);
		Object value = jmeterProperty.getObjectValue();
		if (null == value) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("missing.list.property", displayName);
			throw new IllegalArgumentException(message);
		}

		if (!List.class.isInstance(value)) {
			String displayName = getDisplayName(element, property).orElse(property);
			String type = value.getClass().getName();
			String message = getMessage("invalid.list.property", type, displayName);
			throw new IllegalArgumentException(message);
		}

		List asList = List.class.cast(value);
		if (asList.isEmpty()) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("empty.list.property", displayName);
			throw new IllegalArgumentException(message);
		}
	}

	protected static String getMessage(String code, Object... args) {
		return Messages.getMessage(MESSAGE_SOURCE, code, args);
	}

	public static Optional<PropertyDescriptor> getPropertyDescriptor(TestElement element, String name) {
		checkNotNull(element, "null TestElement");
		checkNotNull(name, "null String");

		Class<? extends TestElement> implementation = element.getClass();
		Optional<PropertyDescriptor> match = Optional.empty();
		try {
			PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(implementation).getPropertyDescriptors();
			match = Arrays.stream(propertyDescriptors)
				.filter(descriptor -> {
					String descriptorName = descriptor.getName();
					return name.equals(descriptorName);
				})
				.findFirst();
		}
		catch (IntrospectionException e) {
			LOGGER.warn("unable to find PropertyDescriptor for {} field {}", implementation, name, e);
		}
		return match;
	}

	@SuppressWarnings("unused")
	public static void assertPopulatedString(TestElement element, String property) throws IllegalArgumentException {
		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null String");

		String value = element.getPropertyAsString(property);
		assertPopulatedString(element, property, value);
	}

	private static void assertPopulatedString(
		TestElement element,
		String property,
		String value
	) throws IllegalArgumentException {
		if (null == value || value.trim().isEmpty()) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("missing.string.property", displayName);
			throw new IllegalArgumentException(message);
		}
	}

	private static void assertPopulatedStringList(TestElement element, String property, Object o) {
		if (null == o) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("missing.string.list.property", displayName);
			throw new IllegalStateException(message);
		}
		else if (!List.class.isInstance(o)) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("invalid.string.list.property", displayName, o.getClass());
			throw new IllegalStateException(message);
		}
		else {
			List asList = List.class.cast(o);
			assertPopulatedStringList(element, property, asList);
		}
	}

	private static void assertPopulatedStringList(TestElement element, String property, List<?> l) {
		boolean evaluation = l.stream()
			.filter(StringProperty.class::isInstance)
			.map(StringProperty.class::cast)
			.map(StringProperty::getStringValue)
			.map(item -> String.class.cast(item).trim())
			.anyMatch(item -> !item.isEmpty());

		if (!evaluation) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("empty.string.list.property", displayName);
			throw new IllegalArgumentException(message);
		}
	}

	@SuppressWarnings("unused")
	public static void assertMaxLength(TestElement element, String property, int maxLength)
		throws IllegalArgumentException {

		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null Property");

		String value = element.getPropertyAsString(property);
		if (null == value || value.trim().isEmpty()) {
			String displayName = Properties.getDisplayName(element, property).orElse(property);
			String message = getMessage("max.length.exceeded", displayName, maxLength);
			throw new IllegalArgumentException(message);
		}
	}

	@SuppressWarnings("unused")
	public static void assertMinimum(TestElement element, String property, int minimum) {
		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null String");

		String stringValue = element.getPropertyAsString(property);
		assertPopulatedString(element, property, stringValue);

		int value = element.getPropertyAsInt(property);
		if (value < minimum) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("invalid.minimum", displayName, stringValue, minimum);
			throw new IllegalArgumentException(message);
		}
	}

	@SuppressWarnings("unused")
	public static void assertMaximum(TestElement element, String property, int maximum) {
		checkNotNull(element, "null TestElement");
		checkNotNull(property, "null String");

		String stringValue = element.getPropertyAsString(property);
		assertPopulatedString(element, property, stringValue);

		int value = element.getPropertyAsInt(property);
		if (value > maximum) {
			String displayName = getDisplayName(element, property).orElse(property);
			String message = getMessage("invalid.maximum", displayName, stringValue, maximum);
			throw new IllegalArgumentException(message);
		}
	}

	public static String[] toNormalizedStringArray(TestElement element, String property) {
		JMeterProperty propertyValue = element.getProperty(property);
		Object o = propertyValue.getObjectValue();
		assertPopulatedStringList(element, property, o);

		List<?> asList = List.class.cast(o);
		return asList.stream()
			.filter(StringProperty.class::isInstance)
			.map(StringProperty.class::cast)
			.map(stringProperty -> stringProperty.getStringValue().trim())
			.filter(item -> !item.isEmpty())
			.toArray(String[]::new);
	}
}