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

package guru.qas.martini.jmeter.control;

import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.config.gui.SimpleConfigGui;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.protocol.java.config.gui.JavaConfigGui;
import org.apache.jmeter.testelement.property.CollectionProperty;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import guru.qas.martini.jmeter.SpringBeanUtil;
import guru.qas.martini.jmeter.config.gui.MartiniBeanConfigGui;

import static guru.qas.martini.jmeter.config.MartiniBeanConfig.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanController extends AbstractMartiniController {

	private static final long serialVersionUID = -3785811213682702141L;

	protected static final ImmutableSet<Class<? extends AbstractConfigGui>> GUIS = ImmutableSet.of(
		MartiniBeanConfigGui.class, JavaConfigGui.class, SimpleConfigGui.class);

	public MartiniBeanController() {
		super();
		setArguments(new Arguments());
	}

	public void setArguments(Arguments arguments) {
		TestElementProperty property = new TestElementProperty(PROPERTY_ARGUMENTS, arguments);
		setProperty(property);
	}

	public void setBeanName(String s) {
		Arguments arguments = getArguments();
		arguments.removeArgument(PROPERTY_BEAN_NAME);
		arguments.addArgument(PROPERTY_BEAN_NAME, s);
	}

	public String getBeanName() {
		Arguments arguments = getArguments();
		Map<String, String> index = arguments.getArgumentsAsMap();
		return index.get(PROPERTY_BEAN_NAME);
	}

	public void setBeanType(String type) {
		setProperty(PROPERTY_BEAN_TYPE, type);
	}

	public String getBeanType() {
		return getPropertyAsString(PROPERTY_BEAN_TYPE);
	}

	public Arguments getArguments() {
		JMeterProperty property = getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : null;
	}

	@Override
	@Nonnull
	protected Controller createDelegate() {
		Controller delegate = createDelegateController();
		Arguments parameters = getArguments();

		Multimap<String, String> index = ArrayListMultimap.create();
		int size = parameters.getArgumentCount();
		for (int i = 0; i < size; i++) {
			Argument argument = parameters.getArgument(i);
			String name = argument.getName();
			String value = argument.getValue();
			if (null != name && null != value) {
				index.put(name, value);
			}
		}

		index.keySet().forEach(k -> {
			Collection<String> values = index.get(k);
			if (1 == values.size()) {
				delegate.setProperty(k, Iterables.getOnlyElement(values));
			}
			else {
				CollectionProperty property = new CollectionProperty(k, values);
				delegate.setProperty(property);
			}
		});
		return delegate;
	}

	protected Controller createDelegateController() {
		String beanName = getBeanName();
		String beanType = getBeanType();
		return SpringBeanUtil.getBean(beanName, beanType, Controller.class);
	}
}