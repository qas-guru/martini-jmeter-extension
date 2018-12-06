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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.NullProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.StringProperty;

import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.gson.JsonArray;

import static com.google.common.base.Preconditions.checkState;

/**
 * Modeled after JavaConfig.
 */
@SuppressWarnings("WeakerAccess")
@Deprecated
public class MartiniBeanConfig extends ConfigTestElement implements Serializable {

	private static final long serialVersionUID = -874529623770552997L;

	protected static final String BEAN_TYPE = "martini.bean.type";
	protected static final String BEAN_NAME = "martini.bean.name";
	protected static final String PARAMETERS = "martini.bean.parameters";

	public MartiniBeanConfig() {
		super();
		setParameters(new Arguments());
	}

	public void setBeanType(String type) {
		setProperty(BEAN_TYPE, type);
	}

	public String getBeanType() {
		return getPropertyAsString(BEAN_TYPE);
	}

	public String getBeanName() {
		Arguments parameters = getParameters();
		String parameter = parameters.getArgumentsAsMap().getOrDefault(BEAN_NAME, "").trim();
		return parameter.isEmpty() ? null : parameter;
	}

	public void setParameters(Arguments arguments) {
		ObjectProperty property = new ObjectProperty(PARAMETERS, arguments);
		super.setProperty(property);
	}

	public Arguments getParameters() {
		JMeterProperty property = getProperty(PARAMETERS);
		Object o = property.getObjectValue();
		checkState(Arguments.class.isInstance(o),
			"property %s not of type %s: %s", PARAMETERS, Arguments.class, null == o ? null : o.getClass());
		return Arguments.class.cast(o);
	}

	public JavaSamplerContext getAsJavaSamplerContext() {
		Arguments arguments = new Arguments();
		LinkedHashMultimap<String, String> index = getIndex();
		Set<String> keys = index.keySet();
		keys.stream().map(k -> {
			Set<String> values = index.get(k);
			int count = values.size();
			String value = null;
			if (1 == count) {
				value = Iterables.getOnlyElement(values);
			}
			else if (1 < count) {
				JsonArray array = new JsonArray();
				values.forEach(array::add);
				value = array.toString();
			}
			return new Argument(k, value);
		}).forEach(arguments::addArgument);
		return new JavaSamplerContext(arguments);
	}

	public List<JMeterProperty> getAsProperties() {
		LinkedHashMultimap<String, String> index = getIndex();
		Set<String> keys = index.keySet();
		return keys.stream().map(k -> {
			Set<String> values = index.get(k);
			int count = values.size();
			JMeterProperty property;
			if (0 == count) {
				property = new NullProperty(k);
			}
			else if (1 == count) {
				property = new StringProperty(k, Iterables.getOnlyElement(values));
			}
			else {
				property = new CollectionProperty(k, values);
			}
			return property;
		}).collect(Collectors.toList());
	}

	protected LinkedHashMultimap<String, String> getIndex() {
		LinkedHashMultimap<String, String> index = LinkedHashMultimap.create();
		Arguments arguments = getParameters();
		int count = arguments.getArgumentCount();
		for (int i = 0; i < count; i++) {
			Argument argument = arguments.getArgument(i);
			String name = argument.getName();
			String value = argument.getValue();
			index.put(name, value);
		}
		return index;
	}

	public Argument getDefaultBeanNameArgument() {
		return new Argument(BEAN_NAME, null, null, "(optional) Spring @Qualifier");
	}
}