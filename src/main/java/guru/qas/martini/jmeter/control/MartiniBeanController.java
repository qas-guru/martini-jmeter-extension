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

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;

import guru.qas.martini.jmeter.SpringBeanUtil;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanController extends AbstractMartiniController {

	private static final long serialVersionUID = -3785811213682702141L;

	protected static final String MARTINI_BEAN_CONFIG = "martini.bean.config";

	public MartiniBeanController() {
		super();
		setConfig(new MartiniBeanConfig());
	}

	public void setConfig(MartiniBeanConfig config) {
		ObjectProperty property = new ObjectProperty(MARTINI_BEAN_CONFIG, config);
		super.setProperty(property);
	}

	public MartiniBeanConfig getConfig() {
		JMeterProperty property = super.getProperty(MARTINI_BEAN_CONFIG);
		Object o = property.getObjectValue();
		checkState(MartiniBeanConfig.class.isInstance(o), "parameter %s is not of type %s: %s",
			MARTINI_BEAN_CONFIG, MartiniBeanConfig.class, null == o ? null : o.getClass());
		return MartiniBeanConfig.class.cast(o);
	}

	@Override
	@Nonnull
	protected Controller createDelegate() {
		Controller delegate = createController();
		MartiniBeanConfig config = getConfig();
		List<JMeterProperty> properties = config.getAsProperties();
		properties.forEach(delegate::setProperty);
		return delegate;
	}

	protected Controller createController() {
		MartiniBeanConfig config = getConfig();
		String beanName = config.getBeanName();
		String beanType = config.getBeanType();
		return SpringBeanUtil.getBean(beanName, beanType, Controller.class);
	}
}