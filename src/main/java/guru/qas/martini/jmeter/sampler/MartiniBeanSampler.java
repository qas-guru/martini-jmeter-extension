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

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.jmeter.protocol.java.sampler.JavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.google.common.base.Throwables;

import guru.qas.martini.Martini;
import guru.qas.martini.jmeter.SpringBeanUtil;
import guru.qas.martini.jmeter.config.MartiniBeanConfig;
import guru.qas.martini.jmeter.control.MartiniScenarioController;
import guru.qas.martini.result.MartiniResult;

import static com.google.common.base.Preconditions.checkState;

/**
 * Modeled after JavaSampler.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class MartiniBeanSampler extends AbstractSampler implements Interruptible {

	private static final long serialVersionUID = 1813073201707263835L;

	private static final String CONFIG = "martini.bean.config";

	protected transient Logger logger;
	protected transient JavaSamplerClient delegate;

	public MartiniBeanSampler() {
		super();
		init();

	}

	protected Object readResolve() {
		init();
		return this;
	}

	protected void init() {
		setConfig(new MartiniBeanConfig());
		logger = LoggerFactory.getLogger(this.getClass());
	}

	public void setConfig(MartiniBeanConfig config) {
		ObjectProperty property = new ObjectProperty(CONFIG, config);
		super.setProperty(property);
	}

	public MartiniBeanConfig getConfig() {
		JMeterProperty property = super.getProperty(CONFIG);
		Object o = property.getObjectValue();
		checkState(MartiniBeanConfig.class.isInstance(o), "parameter %s is not of type %s: %s",
			CONFIG, MartiniBeanConfig.class, null == o ? null : o.getClass());
		return MartiniBeanConfig.class.cast(o);
	}

	@Override
	public SampleResult sample(Entry entry) {
		Martini martini = null;
		SampleResult result;
		try {
			martini = getMartini();

			setDelegate();
			JavaSamplerContext javaSamplerContext = getConfig().getAsJavaSamplerContext();

			delegate.setupTest(javaSamplerContext);
			try {
				result = delegate.runTest(javaSamplerContext);
			}
			finally {
				delegate.teardownTest(javaSamplerContext);
			}
		}
		catch (Exception e) {
			result = new SampleResult();
			result.setStopTestNow(true);
			String stacktrace = Throwables.getStackTraceAsString(e);
			result.setResponseMessage(stacktrace);
		}
		finally {
			destroyDelegate();
		}

		setLabel(martini, result);
		return result;
	}

	@Nullable
	protected Martini getMartini() {
		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		Object o = samplerContext.get(MartiniScenarioController.KEY);
		MartiniResult result = MartiniResult.class.isInstance(o) ? MartiniResult.class.cast(o) : null;
		return null == result ? null : result.getMartini();
	}

	protected void setDelegate() {
		MartiniBeanConfig martiniBeanConfig = getConfig();
		String beanName = martiniBeanConfig.getBeanName();
		String beanType = martiniBeanConfig.getBeanType();
		delegate = SpringBeanUtil.getBean(beanName, beanType, JavaSamplerClient.class);
	}

	protected void destroyDelegate() {
		try {
			DisposableBean disposable = DisposableBean.class.isInstance(delegate) ? DisposableBean.class.cast(delegate) : null;
			if (null != disposable) {
				disposable.destroy();
			}
		}
		catch (Exception e) {
			logger.warn("{}: destroyDelegate() failure", getName(), e);
		}
		finally {
			delegate = null;
		}
	}

	protected void setLabel(@Nullable Martini martini, SampleResult result) {
		if (result != null && result.getSampleLabel().trim().isEmpty()) {
			String label = null == martini ? getName() :
				String.format("%s:%s", martini.getScenarioName(), getName());
			result.setSampleLabel(label);
		}
	}

	@Override
	public boolean interrupt() {
		Interruptible interruptible = Interruptible.class.isInstance(delegate) ? Interruptible.class.cast(delegate) : null;
		return null != interruptible && interruptible.interrupt();
	}
}
