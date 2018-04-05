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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.TestElement;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class DefaultParameterized implements Parameterized {

	protected final TestElement source;
	protected final Arguments arguments;

	public DefaultParameterized(TestElement source, Arguments arguments) {
		this.source = checkNotNull(source, "null TestElement");
		this.arguments = checkNotNull(arguments, "null Arguments");
	}

	@Override
	public TestElement getSource() {
		return source;
	}

	@Override
	public String getSourceName() {
		return source.getName();
	}

	@Override
	public Optional<String> getParameter(@Nonnull String name) {
		checkNotNull(name, "null String");
		Map<String, String> map = null == arguments ? null : arguments.getArgumentsAsMap();
		String parameter = null == map ? null : map.get(name);
		return Optional.ofNullable(parameter);
	}

	@Override
	public Optional<Boolean> getBooleanParameter(@Nonnull String name) {
		checkNotNull(name, "null String");
		String parameter = getNormalized(name);
		Boolean parsed = null == parameter ? null : Boolean.parseBoolean(parameter);
		return Optional.ofNullable(parsed);
	}

	protected String getNormalized(String name) {
		String parameter = getParameter(name).orElse(null);
		String trimmed = null == parameter ? null : parameter.trim();
		return null == trimmed || trimmed.isEmpty() ? null : trimmed;
	}

	@Override
	public Optional<Integer> getIntegerParameter(@Nonnull String name) {
		checkNotNull(name, "null String");
		String parameter = getNormalized(name);
		Integer parsed = null == parameter ? null : Integer.parseInt(parameter);
		return Optional.ofNullable(parsed);
	}

	@Override
	public Optional<Long> getLongParameter(@Nonnull String name) {
		checkNotNull(name, "null String");
		String parameter = getNormalized(name);
		Long parsed = null == parameter ? null : Long.parseLong(parameter);
		return Optional.ofNullable(parsed);
	}

	@Override
	public <T extends Enum<T>> Optional<T> getEnumParameter(@Nonnull Class<T> type, @Nonnull String name) {
		checkNotNull(type, "null Class");
		checkNotNull(name, "null String");
		String parameter = getNormalized(name);
		T cast = null == parameter ? null : Enum.valueOf(type, parameter);
		return Optional.ofNullable(cast);
	}
}
