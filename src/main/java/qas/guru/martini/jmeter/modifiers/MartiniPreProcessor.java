package qas.guru.martini.jmeter.modifiers;

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

	public MartiniPreProcessor() {
		super();
		super.setProperty("text", "my Default Text");
		super.setProperty("contextConfiguration", "meh.xml");
		System.out.println("breakpoint");
	}

//	@SuppressWarnings("MethodDoesntCallSuperMethod")
//	@Override
//	public Object clone() {
//		MartiniPreProcessor processor = new MartiniPreProcessor();
//		for (PropertyIterator i = propertyIterator(); i.hasNext(); ) {
//			JMeterProperty source = i.next();
//			JMeterProperty cloned = source.clone();
//			processor.setProperty(cloned);
//		}
//		processor.setRunningVersion(isRunningVersion()); // TODO: is this actually necessary?
//		return processor;
//	}

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
