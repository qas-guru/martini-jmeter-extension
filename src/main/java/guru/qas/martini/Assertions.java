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

package guru.qas.martini;

import java.util.Map;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.AssertionsMessages.*;

public class Assertions {

	private final String source;

	public Assertions(String source) {
		this.source = checkNotNull(source, "null String").trim();
		checkArgument(!this.source.isEmpty(), "empty String");
	}

	public void assertSet(Map<?, ?> index, String key) {
		checkNotNull(index, "null Map");
		checkNotNull(key, "null String");
		if (!index.containsKey(key)) {
			String message = Messages.getMessage(NOT_SET, source, key);
			throw new IllegalStateException(message);
		}
	}

	public void assertNotNull(String key, @Nullable Object o) {
		checkNotNull(key, "null String");
		if (null == o) {
			String message = Messages.getMessage(NULL_VALUE, source, key);
			throw new IllegalStateException(message);
		}
	}

	public void assertIsInstance(String key, Object o, Class type) {
		checkNotNull(key, "null String");
		checkNotNull(o, "null Object");
		checkNotNull(type, "null Class");

		if (!type.isInstance(o)) {
			String expectedType = type.getName();
			Class<?> actualTypeClass = o.getClass();
			String actualType = actualTypeClass.getName();
			String message = Messages.getMessage(INVALID_TYPE, source, key, expectedType, actualType);
			throw new IllegalArgumentException(message);
		}
	}
}
