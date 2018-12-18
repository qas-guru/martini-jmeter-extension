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

package guru.qas.martini.spring.jmeter;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import guru.qas.martini.jmeter.sampler.BeanSampler;

@SuppressWarnings({"WeakerAccess", "unused"})
@Component("TestSamplerBean")
@Lazy
@Scope("prototype")
public class TestSamplerBean extends AbstractSampler
	implements TestStateListener, LoopIterationListener, BeanSampler, InitializingBean {

	protected Logger logger;

	@Autowired
	protected TestSamplerBean() {
	}

	@Override
	public void afterPropertiesSet() {
		Class<? extends TestSamplerBean> implementation = getClass();
		logger = LoggerFactory.getLogger(implementation);
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		logger.info("Loop started: " + event);
	}

	@Override
	public SampleResult sample(Entry e) {
		SampleResult result = new SampleResult();
		result.setSampleLabel(super.getName());
		result.sampleStart();
		result.sampleEnd();
		result.setSuccessful(true);
		return result;
	}

	@Override
	public void testStarted() {
		logger.info("testStarted()");

	}

	@Override
	public void testStarted(String host) {
		logger.info("testStarted(String): " + host);
	}

	@Override
	public void testEnded() {
		logger.info("testEnded()");
	}

	@Override
	public void testEnded(String host) {
		logger.info("testEnded(String): " + host);
	}
}
