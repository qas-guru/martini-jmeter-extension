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

import org.apache.jmeter.testelement.TestElement;

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
	String getParameter(String name);

	/**
	 * @param name parameter to fetch
	 * @return null if not present or an empty string otherwise populated value
	 */
	String getNormalizedParameter(String name);

	Integer getIntegerParameter(String name) throws NumberFormatException;

	Long getLongParameter(String name) throws NumberFormatException;

	<T extends Enum<T>> T getEnumParameter(Class<T> type, String name);
}