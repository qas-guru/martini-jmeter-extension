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

package guru.qas.martini.jmeter.config;

import java.io.Serializable;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;

/**
 * Modeled after JavaConfig.
 */
public class MartiniBeanConfig extends ConfigTestElement implements Serializable {

	private static final long serialVersionUID = -6043459626018958853L;

	public static final String PROPERTY_BEAN_TYPE = "martini.bean.type";
	public static final String PROPERTY_ARGUMENTS = "martini.bean.arguments";
	public static final String ARGUMENT_BEAN_NAME = "martini.bean.name";

	public MartiniBeanConfig() {
		setArguments(new Arguments());
	}

	public void setBeanType(String type) {
		setProperty(PROPERTY_BEAN_TYPE, type);
	}

	public String getBeanType() {
		return super.getPropertyAsString(PROPERTY_BEAN_TYPE);
	}

	public void setArguments(Arguments args) {
		TestElementProperty property = new TestElementProperty(PROPERTY_ARGUMENTS, args);
		setProperty(property);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : null;
	}
}
