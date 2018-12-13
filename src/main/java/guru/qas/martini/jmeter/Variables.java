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

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ch.qos.cal10n.MessageConveyor;
import guru.qas.martini.Martini;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("WeakerAccess")
public abstract class Variables {

	public static final String MARTINI = Martini.class.getName();
	public static final String SPRING_APPLICATION_CONTEXT = ApplicationContext.class.getName();

	protected static final MessageConveyor MESSAGE_CONVEYOR = new MessageConveyor(JMeterUtils.getLocale());

	private Variables() {
	}

	public static ApplicationContext getSpringApplicationContext() {
		JMeterContext threadContext = JMeterContextService.getContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(SPRING_APPLICATION_CONTEXT);
		checkState(ApplicationContext.class.isInstance(o),
			MESSAGE_CONVEYOR.getMessage(VariablesMessages.SPRING_APPLICATION_CONTEXT_UNAVAILABLE));
		return ClassPathXmlApplicationContext.class.cast(o);
	}
}
