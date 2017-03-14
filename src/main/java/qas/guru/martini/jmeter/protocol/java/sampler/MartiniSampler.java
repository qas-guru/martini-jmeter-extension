package qas.guru.martini.jmeter.protocol.java.sampler;

import java.util.Collection;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import qas.guru.martini.jmeter.modifiers.MartiniConstants;

public class MartiniSampler extends AbstractJavaSamplerClient {

	private static final Logger LOG = LoggingManager.getLoggerFor(MartiniSampler.class.getName());


	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		JMeterContext context = JMeterContextService.getContext();

		JMeterVariables variables = context.getVariables();
		Object object = variables.getObject(MartiniConstants.VALUE_KEY_MIXOLOGIST);
		Mixologist mixologist = Mixologist.class.cast(object);
		Collection<Martini> martinis = mixologist.getMartinis();
		for (Martini martini : martinis) {
			LOG.info("martini: " + martini.toString());
		}

		SampleResult sampleResult = new SampleResult();
		sampleResult.setSuccessful(true);
		return sampleResult;
	}
}
