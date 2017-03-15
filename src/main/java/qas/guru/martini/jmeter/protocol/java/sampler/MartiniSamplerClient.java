package qas.guru.martini.jmeter.protocol.java.sampler;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.event.MartiniEventPublisher;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;
import qas.guru.martini.event.ScenarioEvent;

import static com.google.common.base.Preconditions.checkState;
import static qas.guru.martini.jmeter.modifiers.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSamplerClient extends AbstractJavaSamplerClient {

	private static final Logger LOG = LoggingManager.getLoggerFor(MartiniSamplerClient.class.getName());

	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
	}

	@Override
	public SampleResult runTest(JavaSamplerContext javaSamplerContext) {

		SampleResult result = new SampleResult();
		try {
			Mixologist mixologist = getVariable(VALUE_KEY_MIXOLOGIST, Mixologist.class);
			Collection<Martini> martinis = mixologist.getMartinis();
			for (Martini martini : martinis) {
				LOG.info("Martini: " + martini);
			}
		}
		catch (Exception e) {
			LOG.error("unable to complete sample", e);
			result.setSuccessful(false);
			result.setSamplerData(e.getMessage());
		}
		return result;
	}

	private <T> T getVariable(String key, Class<T> implementation) {
		JMeterContext context = JMeterContextService.getContext();
		JMeterVariables variables = context.getVariables();
		Object object = variables.getObject(key);
		checkState(implementation.isInstance(object), "unable to obtain %s", implementation);
		return implementation.cast(object);
	}

	@Override
	public void teardownTest(JavaSamplerContext context) {
		super.teardownTest(context);
	}
}
