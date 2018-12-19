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

import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.Martini;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.VariablesMessages.*;

@SuppressWarnings("WeakerAccess")
public abstract class Variables {

	public static final String MARTINI = Martini.class.getName();
	public static final String SPRING_APPLICATION_CONTEXT = ApplicationContext.class.getName();
	public static final String SUITE_IDENTIFIER = SuiteIdentifier.class.getName();
	public static final String MARTINI_RESULT = MartiniResult.class.getName();

	protected static final MessageConveyor MESSAGE_CONVEYOR = new MessageConveyor(JMeterUtils.getLocale());

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

	public static void set(String key, Object value) {
		JMeterVariables variables = getVariables();
		variables.putObject(key, value);
	}

	protected static JMeterVariables getVariables() {
		JMeterContext threadContext = JMeterContextService.getContext();
		return threadContext.getVariables();
	}

	public static int getIteration() {
		JMeterVariables variables = getVariables();
		return variables.getIteration();
	}

	public static ConfigurableApplicationContext getSpringApplicationContext() {
		Object o = getVariable(SPRING_APPLICATION_CONTEXT);
		checkNotNull(o, MESSAGE_CONVEYOR.getMessage(SPRING_APPLICATION_CONTEXT_NOT_SET));
		checkState(ConfigurableApplicationContext.class.isInstance(o),
			MESSAGE_CONVEYOR.getMessage(
				INVALID_SPRING_APPLICATION_CONTEXT_INSTANCE,
				ConfigurableApplicationContext.class,
				o.getClass().getName()));
		return ConfigurableApplicationContext.class.cast(o);
	}

	public static Object getVariable(String key) {
		JMeterVariables variables = getVariables();
		return variables.getObject(key);
	}

	public static Optional<Martini> getMartini() {
		Object o = getVariable(MARTINI);
		checkState(null == o || Martini.class.isInstance(o),
			MESSAGE_CONVEYOR.getMessage(INVALID_MARTINI_INSTANCE, Martini.class, null == o ? null : o.getClass()));
		return null == o ? Optional.empty() : Optional.of(Martini.class.cast(o));
	}
}