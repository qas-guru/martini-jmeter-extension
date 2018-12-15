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

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.Martini;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.VariablesMessages.*;

@SuppressWarnings("WeakerAccess")
public abstract class Variables {

	public static final String MARTINI = Martini.class.getName();
	public static final String SPRING_APPLICATION_CONTEXT = ApplicationContext.class.getName();

	protected static final MessageConveyor MESSAGE_CONVEYOR = new MessageConveyor(JMeterUtils.getLocale());

	private Variables() {
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
		JMeterContext threadContext = JMeterContextService.getContext();
		JMeterVariables variables = threadContext.getVariables();
		return variables.getObject(key);
	}

	public static Optional<Martini> getMartini() {
		Object o = getVariable(MARTINI);
		boolean isMartini = Martini.class.isInstance(o);
		checkState(null == o || isMartini,
			MESSAGE_CONVEYOR.getMessage(INVALID_MARTINI_INSTANCE, Martini.class, null == o ? null : o.getClass()));
		return isMartini ? Optional.of(Martini.class.cast(o)) : Optional.empty();
	}
}
