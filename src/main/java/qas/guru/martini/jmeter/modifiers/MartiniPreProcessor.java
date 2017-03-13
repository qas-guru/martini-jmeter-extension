package qas.guru.martini.jmeter.modifiers;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	private static final Logger LOG = LoggingManager.getLoggerForClass();

	public static final String JMETER_CONTEXT_KEY = "martiniPreProcessor";
	public static final String DEFAULT_CONTEXT_CONFIGURATION = "application.xml";
	public static final String DEFAULT_TEXT = "Stirred, not shaken.";

	protected final AtomicReference<String> textReference;
	protected final AtomicReference<String> contextConfigurationReference;

	public MartiniPreProcessor() {
		this(new AtomicReference<>(DEFAULT_TEXT), new AtomicReference<>(DEFAULT_CONTEXT_CONFIGURATION));
	}

	protected MartiniPreProcessor(
		AtomicReference<String> textReference,
		AtomicReference<String> contextConfigurationReference
	) {
		this.textReference = textReference;
		this.contextConfigurationReference = contextConfigurationReference;
	}

	public String getText() {
		return textReference.get();
	}

	public String getContextConfiguration() {
		return contextConfigurationReference.get();
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		MartiniPreProcessor processor = new MartiniPreProcessor(textReference, contextConfigurationReference);
		for (PropertyIterator i = propertyIterator(); i.hasNext(); ) {
			JMeterProperty source = i.next();
			JMeterProperty cloned = source.clone();
			processor.setProperty(cloned);
		}
		processor.setRunningVersion(isRunningVersion());
		return processor;
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
