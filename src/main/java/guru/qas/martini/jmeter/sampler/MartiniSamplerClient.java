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

package guru.qas.martini.jmeter.sampler;

import java.io.Serializable;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.MessageSource;

import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.SpringBeanUtil;

@SuppressWarnings({"WeakerAccess", "unused"})
public class MartiniSamplerClient implements Serializable, Interruptible, JavaSamplerClient {

	protected static final String PARAMETER_BEAN_NAME = "martini.sampler.client.bean.name";
	protected static final String PARAMETER_BEAN_TYPE = "martini.sampler.client.bean.type";
	private static final long serialVersionUID = -7760439824003903354L;

	protected transient MessageSource messageSource;
	protected transient Arguments arguments;
	protected transient JavaSamplerClient delegate;
	protected transient Interruptible asInterruptible;

	public MartiniSamplerClient() {
		super();
		init();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	protected void init() {
		messageSource = MessageSources.getMessageSource(getClass());
		arguments = new Arguments();
		arguments.addArgument(PARAMETER_BEAN_NAME, "mySamplerClientBean");
		arguments.addArgument(PARAMETER_BEAN_TYPE, "com.mine.MySamplerClientBean");
	}

	@Override
	public Arguments getDefaultParameters() {
		return arguments;
	}

	@Override
	public void setupTest(JavaSamplerContext context) {
		String name = getNormalized(context, PARAMETER_BEAN_NAME);
		String type = getNormalized(context, PARAMETER_BEAN_TYPE);
		assertBeanIdentified(context, name, type);
		delegate = SpringBeanUtil.getBean(name, type, JavaSamplerClient.class);
		asInterruptible = Interruptible.class.isInstance(delegate) ? Interruptible.class.cast(delegate) : null;
		delegate.setupTest(context);
	}

	protected void assertBeanIdentified(JavaSamplerContext context, String name, String type) {
		if (null == name && null == type) {
			context.getJMeterContext().getEngine().stopTest(true);
			Sampler testElement = context.getJMeterContext().getCurrentSampler();
			String message = messageSource.getMessage(
				"error.retrieving.bean", new Object[]{PARAMETER_BEAN_NAME, PARAMETER_BEAN_TYPE}, JMeterUtils.getLocale());
			Gui.reportError(testElement, message);
		}
	}

	protected static String getNormalized(JavaSamplerContext context, String key) {
		String parameter = context.getParameter(key);
		String trimmed = null == parameter ? "" : parameter.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		return delegate.runTest(context);
	}

	@Override
	public void teardownTest(JavaSamplerContext context) {
		delegate = null;
		asInterruptible = null;
	}

	@Override
	public boolean interrupt() {
		return null != asInterruptible && asInterruptible.interrupt();
	}
}
