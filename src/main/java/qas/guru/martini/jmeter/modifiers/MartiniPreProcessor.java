package qas.guru.martini.jmeter.modifiers;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.util.concurrent.Monitor;

import static qas.guru.martini.jmeter.modifiers.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final Logger LOG = LoggingManager.getLoggerForClass();
	public static final String DEFAULT_SPRING_CONTEXT = "applicationContext.xml";

	public MartiniPreProcessor() {
		super();
		super.setProperty(PROPERTY_KEY_SPRING_CONTEXT, DEFAULT_SPRING_CONTEXT);
		ObjectProperty monitorProperty = new ObjectProperty(PROPERTY_KEY_MONITOR, new Monitor());
		super.setProperty(monitorProperty);
	}

	@Override
	public void process() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}

		JMeterProperty monitor = super.getProperty(PROPERTY_KEY_MONITOR);
		variables.putObject(VALUE_KEY_MONITOR, monitor);
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
