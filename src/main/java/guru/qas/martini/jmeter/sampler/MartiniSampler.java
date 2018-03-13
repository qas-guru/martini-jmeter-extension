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

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import guru.qas.martini.Martini;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.harness.MartiniCallable;

import static com.google.common.base.Preconditions.checkState;
import static guru.qas.martini.jmeter.Constants.*;

public class MartiniSampler extends AbstractSampler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniSampler.class);

	@Override
	public SampleResult sample(Entry e) {
		SampleResult result = new SampleResult();
		result.setDataType(SampleResult.TEXT);
		result.setSampleLabel(getName());
		result.sampleStart();

		try {
			ApplicationContext springContext = getFromSamplerContext(ApplicationContext.class, KEY_SPRING_CONTEXT);
			AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
			Martini martini = getFromSamplerContext(Martini.class, KEY_CURRENT_MARTINI);
			SuiteIdentifier suiteIdentifier = springContext.getBean(SuiteIdentifier.class);
			MartiniCallable callable = new MartiniCallable(suiteIdentifier, martini);
			beanFactory.autowireBean(callable);
			MartiniResult martiniResult = callable.call();
			System.out.println("breakpoint");
		}
		catch (Exception exception) {
			System.out.println("oopsy");
		}

		// Execute the martini task
		// put results in SampleResult

		result.sampleEnd();
		result.setSuccessful(true);
		return result;
	}

	private <T> T getFromSamplerContext(Class<T> implementation, String key) {
		Map<String, Object> samplerContext = getSamplerContext();
		Object o = samplerContext.get(key);
		checkState(implementation.isInstance(o), "object of type %s not present in SamplerContext under key %s", implementation, key);
		return implementation.cast(o);
	}

	private Map<String, Object> getSamplerContext() {
		JMeterContext threadContext = super.getThreadContext();
		return threadContext.getSamplerContext();
	}
}
