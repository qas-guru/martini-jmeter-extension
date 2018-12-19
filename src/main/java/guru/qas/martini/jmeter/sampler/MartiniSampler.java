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

import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;

@SuppressWarnings("RedundantThrows")
public class MartiniSampler extends AbstractGenericSampler
	implements Serializable, Cloneable, TestStateListener, TestBean {

	public MartiniSampler() {
		super();
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() throws Exception {
		return new MartiniSamplerBeanInfoSupport();
	}

	@Override
	protected void completeSetup() throws Exception {
	}

	@Override
	protected void beginTearDown() throws Exception {
	}

	@Override
	public SampleResult sample(Entry e) {
		return null;
	}
}
