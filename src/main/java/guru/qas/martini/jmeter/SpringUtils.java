/*
Copyright 2017 Penny Rohr Curich

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

import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ApplicationContext;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.SpringPreProcessor.KEY_APPLICATION_CONTEXT;

@SuppressWarnings("WeakerAccess")
public final class SpringUtils {

	private static final Class<? extends ApplicationContext> IMPLEMENTATION = ApplicationContext.class;

	private SpringUtils() {
	}

	public static ApplicationContext getApplicationContext(JMeterVariables variables) {
		Object o = variables.getObject(KEY_APPLICATION_CONTEXT);
		checkNotNull(o, "no variable found for key %s", KEY_APPLICATION_CONTEXT);
		checkState(IMPLEMENTATION.isInstance(o),
			"variable %s not of expected type %s, found %s", KEY_APPLICATION_CONTEXT, IMPLEMENTATION, o.getClass());
		return IMPLEMENTATION.cast(o);
	}
}
