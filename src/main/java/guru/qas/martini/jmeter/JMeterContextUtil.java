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

import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class JMeterContextUtil {

	public static <T> Optional<T> getProperty(@Nonnull TestElement element, @Nonnull Class<T> implementation) {
		checkNotNull(element, "null TestElement");
		checkNotNull(implementation, "null Class");

		String name = getKey(implementation);
		JMeterProperty property = element.getProperty(name);
		Object o = property.getObjectValue();
		return getOptional(o, implementation);
	}

	protected static <T> Optional<T> getOptional(Object o, Class<T> implementation) {
		T instance = implementation.isInstance(o) ? implementation.cast(o) : null;
		return Optional.ofNullable(instance);
	}

	public static <T> Optional<T> getVariable(@Nonnull Class<T> implementation) {
		checkNotNull(implementation, "null Class");
		Object o = getVariableObject(implementation);
		return getOptional(o, implementation);
	}

	protected static Object getVariableObject(Class implementation) {
		JMeterVariables variables = getVariables();
		String key = getKey(implementation);
		return variables.getObject(key);
	}

	protected static JMeterVariables getVariables() {
		JMeterContext context = JMeterContextService.getContext();
		return context.getVariables();
	}

	public static String getKey(Class implementation) {
		return String.format("martini.%s", implementation.getName());
	}

	public static void setVariable(@Nullable Object o, @Nonnull Class implementation) {
		checkNotNull(implementation, "null Class");

		JMeterVariables variables = getVariables();
		String key = getKey(implementation);
		if (null == o) {
			variables.remove(key);
		}
		else {
			variables.putObject(key, o);
		}
	}

	public static void removeVariable(@Nonnull Class implementation) {
		checkNotNull(implementation, "null Class");
		JMeterVariables variables = getVariables();
		String key = getKey(implementation);
		variables.remove(key);
	}

	public static void setSamplerData(@Nonnull Sampler sampler, @Nullable Object o, @Nonnull Class implementation) {
		checkNotNull(sampler, "null Sampler");
		checkNotNull(implementation, "null Class");
		JMeterContext threadContext = sampler.getThreadContext();
		Map<String, Object> map = threadContext.getSamplerContext();
		String key = getKey(implementation);
		if (null == o) {
			map.remove(key);
		}
		else {
			map.put(key, o);
		}
	}

	public static void setSamplerData(@Nullable Object o, @Nonnull Class implementation) {
		checkNotNull(implementation, "null Class");
		JMeterContext context = JMeterContextService.getContext();
		Sampler currentSampler = context.getCurrentSampler();
		setSamplerData(currentSampler, o, implementation);
	}

	public static void removeSamplerData(@Nonnull Class implementation) {
		checkNotNull(implementation, "null Class");
		JMeterContext context = JMeterContextService.getContext();
		Map<String, Object> map = context.getSamplerContext();
		String key = getKey(implementation);
		map.remove(key);
	}

	public static <T> Optional<T> getSamplerData(@Nonnull Class<T> implementation) {
		JMeterContext context = JMeterContextService.getContext();
		Map<String, Object> map = context.getSamplerContext();
		Object o = null == map ? null : map.get(getKey(implementation));
		T instance = implementation.isInstance(o) ? implementation.cast(o) : null;
		return Optional.ofNullable(instance);
	}

	public static void setTemporaryProperty(
		@Nonnull TestElement element,
		@Nullable Object o,
		@Nonnull Class implementation
	) {
		checkNotNull(element, "null TestElement");
		checkNotNull(implementation, "null Class");

		String key = getKey(implementation);
		ObjectProperty property = new ObjectProperty(key, o);
		element.setProperty(property);
		element.setTemporary(property);
	}

	public static void removeProperty(@Nonnull TestElement element, @Nonnull Class implementation) {
		checkNotNull(element, "null TestElement");
		checkNotNull(implementation, "null Class");
		String key = getKey(implementation);
		element.removeProperty(key);
	}
}
