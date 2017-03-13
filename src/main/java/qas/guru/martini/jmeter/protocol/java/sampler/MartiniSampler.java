package qas.guru.martini.jmeter.protocol.java.sampler;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

import static qas.guru.martini.jmeter.modifiers.MartiniPreProcessor.JMETER_CONTEXT_KEY;

public class MartiniSampler extends AbstractJavaSamplerClient {

	private static final Logger LOG = LoggingManager.getLoggerForClass();

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		JMeterContext context = JMeterContextService.getContext();

		JMeterVariables variables = context.getVariables();
		Object object = variables.getObject(JMETER_CONTEXT_KEY);
		System.out.println(object);

		SampleResult sampleResult = new SampleResult();

		if (MartiniPreProcessor.class.isInstance(object)) {
			MartiniPreProcessor cast = MartiniPreProcessor.class.cast(object);
			int id = System.identityHashCode(cast);
			LOG.info("identity of processor object is " + id);

			String configuration = cast.getContextConfiguration();
			LOG.info("configured Spring context is " + configuration);
			sampleResult.setSuccessful(true);
		}
		else {
			sampleResult.setSuccessful(false);
			System.out.println("I don't know what to do");
		}

		return sampleResult;
	}
}
