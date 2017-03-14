package qas.guru.martini.jmeter.protocol.java.sampler;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.util.concurrent.Monitor;

import qas.guru.martini.jmeter.modifiers.MartiniConstants;

public class MartiniSampler extends AbstractJavaSamplerClient {

	private static final Logger LOG = LoggingManager.getLoggerForClass();

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {
		JMeterContext context = JMeterContextService.getContext();

		JMeterVariables variables = context.getVariables();
		Object object = variables.getObject(MartiniConstants.PROPERTY_KEY_MONITOR);
		System.out.println(object);

		SampleResult sampleResult = new SampleResult();

		if (Monitor.class.isInstance(object)) {
			Monitor monitor = Monitor.class.cast(object);
			LOG.info("monitor is " + monitor);
		}
		return sampleResult;
	}
}
