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

import javax.annotation.Nonnull;

import org.apache.jmeter.testelement.TestElement;

@SuppressWarnings("unused")
public interface Parameterized {

	TestElement getSource();

	/**
	 * @return name of TestElement
	 */
	String getSourceName();

	/**
	 * @param name parameter to fetch
	 * @return null if not present or set to null, otherwise value
	 */
	Optional<String> getParameter(@Nonnull String name);

	Optional<Boolean> getBooleanParameter(@Nonnull String name);

	Optional<Integer> getIntegerParameter(@Nonnull String name) throws NumberFormatException;

	Optional<Long> getLongParameter(@Nonnull String name) throws NumberFormatException;

	<T extends Enum<T>> Optional<T> getEnumParameter(@Nonnull Class<T> type, @Nonnull String name);
}