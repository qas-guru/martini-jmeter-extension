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

import javax.annotation.Nonnull;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.control.Controller;

import guru.qas.martini.jmeter.DefaultParameterized;
import guru.qas.martini.jmeter.JMeterContextUtil;
import guru.qas.martini.jmeter.Parameterized;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniController extends AbstractMartiniController {

	private static final long serialVersionUID = -3785811213682702141L;
	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	public MartiniController() {
		super();
	}

	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	@Override
	@Nonnull
	protected Controller createDelegate() {
		return new DelegateMartiniController();
	}

	protected void initializeDelegate() {
		super.initializeDelegate();

		Arguments arguments = new Arguments();
		arguments.addArgument(PROPERTY_SPEL_FILTER, getSpelFilter());
		DefaultParameterized parameterized = new DefaultParameterized(this, arguments);
		JMeterContextUtil.setTemporaryProperty(delegate, parameterized, Parameterized.class);
	}
}