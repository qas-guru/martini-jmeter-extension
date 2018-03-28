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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.TestElement;

import com.google.common.collect.ImmutableMap;

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
	public String getParameter(String name) {
		checkNotNull(name, "null String");
		Map<String, String> map = null == arguments ? ImmutableMap.of() : arguments.getArgumentsAsMap();
		return map.get(name);
	}

	@Override
	public String getNormalizedParameter(String name) {
		checkNotNull(name, "null String");
		String parameter = getParameter(name);
		String trimmed = null == parameter ? "" : parameter.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	@Override
	public Integer getIntegerParameter(String name) {
		checkNotNull(name, "null String");
		String parameter = getNormalizedParameter(name);
		return null == parameter ? null : Integer.parseInt(parameter);
	}

	@Override
	public Long getLongParameter(String name) {
		checkNotNull(name, "null String");
		String parameter = getNormalizedParameter(name);
		return null == parameter ? null : Long.parseLong(parameter);
	}

	@Override
	public <T extends Enum<T>> T getEnumParameter(Class<T> type, String name) {
		checkNotNull(type, "null Class");
		checkNotNull(name, "null String");
		String parameter = getNormalizedParameter(name);
		return null == parameter ? null : Enum.valueOf(type, parameter);
	}
}
