package qas.guru.martini.jmeter.protocol.java.sampler;

import java.util.Date;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import qas.guru.martini.jmeter.modifiers.MartiniPreProcessor;

public final class MartiniSampler extends AbstractJavaSamplerClient {

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		JMeterContext context = JMeterContextService.getContext();

		JMeterVariables variables = context.getVariables();
		Object object = variables.getObject(MartiniPreProcessor.CONTEXT_KEY);
		System.out.println(object);

		SampleResult sampleResult = new SampleResult();

		if (MartiniPreProcessor.class.isInstance(object)) {
			MartiniPreProcessor cast = MartiniPreProcessor.class.cast(object);
			int id = System.identityHashCode(cast);
			System.out.println("identity of processor object is " + id);
			Date timestamp = cast.getTimestamp();
			System.out.println("Its timestamp is " + timestamp + " with system ID " + System.identityHashCode(timestamp));
			sampleResult.setSuccessful(true);
		}
		else {
			sampleResult.setSuccessful(false);
			System.out.println("I don't know what to do");
		}

		return sampleResult;
	}
}
