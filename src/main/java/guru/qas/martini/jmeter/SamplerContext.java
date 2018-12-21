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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.springframework.context.ConfigurableApplicationContext;

import guru.qas.martini.Assertions;
import guru.qas.martini.Martini;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;

import static guru.qas.martini.jmeter.Variables.*;

@SuppressWarnings("WeakerAccess")
public abstract class SamplerContext {

	private SamplerContext() {
	}

	public static void set(@Nullable ConfigurableApplicationContext c) {
		set(Variables.SPRING_APPLICATION_CONTEXT, c);
	}

	public static void set(@Nullable SuiteIdentifier i) {
		set(Variables.SUITE_IDENTIFIER, i);
	}

	public static void set(@Nullable Martini m) {
		set(MARTINI, m);
	}

	public static void set(@Nullable MartiniResult r) {
		set(MARTINI_RESULT, r);
	}

	protected static void set(String key, Object value) {
		Map<String, Object> samplerContext = getSamplerContext();
		if (null == value) {
			samplerContext.remove(key);
		}
		else {
			samplerContext.put(key, value);
		}
	}

	protected static Map<String, Object> getSamplerContext() {
		JMeterContext context = JMeterContextService.getContext();
		return context.getSamplerContext();
	}

	@Nonnull
	public static Martini getMartini() {
		return getValue(MARTINI, Martini.class);
	}

	@Nonnull
	public static ConfigurableApplicationContext getSpringApplicationContext() {
		return getValue(SPRING_APPLICATION_CONTEXT, ConfigurableApplicationContext.class);
	}

	protected static <T> T getValue(String key, Class<T> type) {
		Map<String, Object> samplerContext = getSamplerContext();
		Assertions assertions = new Assertions(SamplerContext.class.getSimpleName());
		assertions.assertSet(samplerContext, key);
		Object o = samplerContext.get(key);
		assertions.assertNotNull(key, o);
		assertions.assertIsInstance(key, o, type);
		return type.cast(o);
	}
}
