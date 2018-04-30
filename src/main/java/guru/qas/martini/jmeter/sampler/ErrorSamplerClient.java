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

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modeled after JavaSamplerClient.ErrorSamplerClient
 */
@SuppressWarnings("WeakerAccess")
public class ErrorSamplerClient extends AbstractJavaSamplerClient implements MartiniBeanSamplerClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(ErrorSamplerClient.class);

	private final Class implementation;
	private final String description;

	public ErrorSamplerClient(Class implementation, String description) {
		this.implementation = implementation;
		this.description = description;
	}

	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		LOGGER.debug("{}\trunTest", description);
		Thread.yield();
		SampleResult results = new SampleResult();
		results.setSuccessful(false);
		results.setResponseData("Class not found: " + implementation, null);
		results.setSampleLabel("ERROR: " + implementation);
		return results;
	}
}
