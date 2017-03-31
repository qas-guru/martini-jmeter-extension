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

package guru.qas.martini.jmeter.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import com.google.common.base.Splitter;

import static com.google.common.base.Preconditions.*;
import static org.springframework.beans.factory.config.PropertyPlaceholderConfigurer.SYSTEM_PROPERTIES_MODE_FALLBACK;

@SuppressWarnings("WeakerAccess")
public class DefaultApplicationContextBuilder implements ApplicationContextBuilder {

	protected JMeterProperty configLocations;
	protected JMeterProperty profiles;
	protected JMeterProperty environment;

	@Override
	public DefaultApplicationContextBuilder setConfigLocations(JMeterProperty property) {
		configLocations = property;
		return this;
	}

	@Override
	public DefaultApplicationContextBuilder setProfiles(JMeterProperty property) {
		profiles = property;
		return this;
	}

	@Override
	public DefaultApplicationContextBuilder setEnvironment(JMeterProperty property) {
		environment = property;
		return this;
	}

	@Override
	public ConfigurableApplicationContext build() {
		String[] locations = getStringArray(configLocations);
		checkNotNull(configLocations, "configLocations not populated");
		return build(locations);
	}

	@SuppressWarnings("ConstantConditions")
	protected String[] getStringArray(JMeterProperty property) {
		Object asObject = property.getObjectValue();
		checkState(null == asObject || String.class.isInstance(asObject),
			"expected %s to contain a String but found %s", property.getName(), asObject.getClass());
		return null == asObject ? null : getStringArray(String.class.cast(asObject));
	}

	protected String[] getStringArray(String value) {
		List<String> asList = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(value);
		return asList.toArray(new String[asList.size()]);
	}

	protected ConfigurableApplicationContext build(String[] locations) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(locations, false);
		setProfiles(context);
		setEnvironment(context);
		context.refresh();
		return context;
	}

	protected void setProfiles(ConfigurableApplicationContext context) {
		String[] profileArray = getStringArray(profiles);
		if (null != profileArray && 0 < profileArray.length) {
			ConfigurableEnvironment environment = context.getEnvironment();
			environment.setActiveProfiles(profileArray);
		}
	}

	@SuppressWarnings("ConstantConditions")
	protected void setEnvironment(ConfigurableApplicationContext context) {
		Object asObject = null == environment ? null : environment.getObjectValue();
		checkState(null == asObject || Arguments.class.isInstance(asObject),
			"expected %s to contain Arguments but found %s", environment.getName(), asObject.getClass());

		Properties properties = getProperties(Arguments.class.cast(asObject));
		setEnvironment(context, properties);

	}

	protected Properties getProperties(Arguments arguments) {
		Properties properties = new Properties();
		Map<String, String> asMap = null == arguments ? Collections.emptyMap() : arguments.getArgumentsAsMap();
		properties.putAll(asMap);
		return properties;
	}

	protected void setEnvironment(ConfigurableApplicationContext context, Properties properties) {
		PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
		configurer.setProperties(properties);
		configurer.setLocalOverride(true);
		configurer.setSearchSystemEnvironment(true);
		configurer.setSystemPropertiesMode(SYSTEM_PROPERTIES_MODE_FALLBACK);
		context.addBeanFactoryPostProcessor(configurer);
	}
}
