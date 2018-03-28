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

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.Controller;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.testelement.property.TestElementProperty;

import guru.qas.martini.jmeter.DefaultParameterized;
import guru.qas.martini.jmeter.Parameterized;
import guru.qas.martini.jmeter.SpringBeanUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniBeanController extends AbstractMartiniController {

	private static final long serialVersionUID = -3785811213682702141L;

	protected static final String PROPERTY_BEAN_NAME = "martini.bean.controller.bean.name";
	protected static final String PROPERTY_BEAN_TYPE = "martini.bean.controller.bean.type";
	protected static final String PROPERTY_ARGUMENTS = "martini.bean.controller.arguments";

	public MartiniBeanController() {
		super();
	}

	public void setBeanName(String s) {
		super.setProperty(PROPERTY_BEAN_NAME, null == s ? null : s.trim());
	}

	public String getBeanName() {
		return super.getPropertyAsString(PROPERTY_BEAN_NAME).trim();
	}

	public void setBeanType(String s) {
		super.setProperty(PROPERTY_BEAN_TYPE, null == s ? null : s.trim());
	}

	public String getBeanType() {
		return super.getPropertyAsString(PROPERTY_BEAN_TYPE.trim());
	}

	public void setArguments(Arguments a) {
		TestElementProperty property = new TestElementProperty(PROPERTY_ARGUMENTS, a);
		super.setProperty(property);
	}

	public Arguments getArguments() {
		JMeterProperty property = super.getProperty(PROPERTY_ARGUMENTS);
		Object o = property.getObjectValue();
		return Arguments.class.isInstance(o) ? Arguments.class.cast(o) : null;
	}

	protected void initializeDelegate() {
		super.initializeDelegate();
		Arguments parameters = getArguments();
		DefaultParameterized parameterized = new DefaultParameterized(this, null == parameters ? new Arguments() : parameters);
		ObjectProperty property = new ObjectProperty(Parameterized.class.getName(), parameterized);
		delegate.setProperty(property);
		delegate.setTemporary(property);
	}

	@Override
	protected Controller createDelegate() {
		String beanName = getBeanName();
		String beanType = getBeanType();
		return SpringBeanUtil.getBean(beanName, beanType, Controller.class);
	}
}