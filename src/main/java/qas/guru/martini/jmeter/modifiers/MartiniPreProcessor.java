package qas.guru.martini.jmeter.modifiers;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final Logger LOG = LoggingManager.getLoggerForClass();
	public static final String JMETER_CONTEXT_KEY = "martiniPreProcessor";
	public static final String PROPERTY_TEXT_KEY = "text";
	public static final String PROPERTY_TEXT_VALUE_DEFAULT = "Stirred, not shaken.";
	public static final String PROPERTY_CONTEXT_CONFIGURATION_KEY = "contextConfiguration";
	public static final String PROPERTY_CONTEXT_CONFIGURATION_VALUE_DEFAULT = "applicationContext.xml";

	public MartiniPreProcessor() {
		super();
		super.setProperty(PROPERTY_TEXT_KEY, PROPERTY_TEXT_VALUE_DEFAULT);
		super.setProperty(PROPERTY_CONTEXT_CONFIGURATION_KEY, PROPERTY_CONTEXT_CONFIGURATION_VALUE_DEFAULT);
	}

	@Override
	public void process() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}
		variables.putObject(JMETER_CONTEXT_KEY, this);
	}

	@Override
	public void testStarted() {
		LOG.info("Hello, Martini!");
		// TODO: HERE, we start up Spring!
	}

	@Override
	public void testStarted(String s) {
	}

	@Override
	public void testEnded() {
	}

	@Override
	public void testEnded(String s) {
	}
}
