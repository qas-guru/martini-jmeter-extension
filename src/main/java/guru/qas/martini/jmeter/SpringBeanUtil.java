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

import javax.annotation.Nonnull;

import org.springframework.context.ApplicationContext;

import static com.google.common.base.Preconditions.checkNotNull;
import static guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor.getSpringContext;

@SuppressWarnings("WeakerAccess")
public class SpringBeanUtil {

	public static <T> T getBean(String name, String type, @Nonnull Class<T> expectedType) {
		checkNotNull(expectedType, "null Class");

		ApplicationContext context = getSpringContext().orElseThrow(NullPointerException::new);
		Class<? extends T> implementation = null == type ? null : getImplementation(context, type);

		Object bean;
		if (null == name && null == implementation) {
			throw new RuntimeException("at least one of bean name or bean type must be specified");
		}
		else if (null == name) {
			bean = context.getBean(implementation);
		}
		else if (null == implementation) {
			bean = context.getBean(name);
		}
		else {
			bean = context.getBean(name, implementation);
		}
		return expectedType.cast(bean);
	}

	protected static Class getImplementation(ApplicationContext springContext, String type) {
		ClassLoader loader = springContext.getClassLoader();
		try {
			return Class.forName(type, true, loader);
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}