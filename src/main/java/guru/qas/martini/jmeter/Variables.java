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

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.collect.ImmutableMap;

import guru.qas.martini.Assertions;
import guru.qas.martini.Martini;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;

@SuppressWarnings("WeakerAccess")
public abstract class Variables {

	protected static final Assertions ASSERTIONS = new Assertions(JMeterVariables.class.getSimpleName());

	public static final String MARTINI = Martini.class.getName();
	public static final String SPRING_APPLICATION_CONTEXT = ConfigurableApplicationContext.class.getName();
	public static final String SUITE_IDENTIFIER = SuiteIdentifier.class.getName();
	public static final String MARTINI_RESULT = MartiniResult.class.getName();

	private Variables() {
	}

	public static void set(@Nullable ConfigurableApplicationContext c) {
		set(SPRING_APPLICATION_CONTEXT, c);
	}

	public static void set(@Nullable SuiteIdentifier i) {
		set(SUITE_IDENTIFIER, i);
	}

	public static void set(@Nullable Martini martini) {
		set(MARTINI, martini);
	}

	public static void set(@Nullable MartiniResult r) {
		set(MARTINI_RESULT, r);
	}

	protected static void set(String key, @Nullable Object value) {
		JMeterVariables variables = getVariables();
		if (null == value) {
			variables.remove(key);
		}
		else {
			variables.putObject(key, value);
		}
	}

	protected static JMeterVariables getVariables() {
		JMeterContext threadContext = JMeterContextService.getContext();
		return threadContext.getVariables();
	}

	public static int getIteration() {
		JMeterVariables variables = getVariables();
		return variables.getIteration();
	}

	@Nonnull
	public static ConfigurableApplicationContext getSpringApplicationContext() {
		return getVariable(SPRING_APPLICATION_CONTEXT, ConfigurableApplicationContext.class);
	}

	public static Optional<Martini> getOptionalMartini() {
		return getOptionalVariable(MARTINI, Martini.class);
	}

	@Nonnull
	protected static <T> T getVariable(String key, Class<T> type) {
		Map<String, Object> index = getAsMap();
		ASSERTIONS.assertSet(index, key);
		Object o = index.get(key);
		ASSERTIONS.assertNotNull(key, o);
		ASSERTIONS.assertIsInstance(key, o, type);
		return type.cast(o);
	}

	protected static <T> Optional<T> getOptionalVariable(String key, Class<T> type) {
		Map<String, Object> index = getAsMap();

		T variable = null;
		if (index.containsKey(key)) {
			Object o = index.get(key);
			ASSERTIONS.assertNotNull(key, o);
			ASSERTIONS.assertIsInstance(key, o, type);
			variable = type.cast(o);
		}
		return Optional.ofNullable(variable);
	}

	public static ImmutableMap<String, Object> getAsMap() {
		JMeterVariables variables = getVariables();
		ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
		variables.getIterator().forEachRemaining(builder::put);
		return builder.build();
	}
}