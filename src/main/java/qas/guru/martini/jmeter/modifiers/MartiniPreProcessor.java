package qas.guru.martini.jmeter.modifiers;

import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.PropertyIterator;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;

public final class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {
	public static final String CONTEXT_KEY = "martiniPreProcessor";

	private final AtomicReference<Date> reference;

	public MartiniPreProcessor() {
		this(new AtomicReference<>());
	}

	private MartiniPreProcessor(AtomicReference<Date> reference) {
		this.reference = reference;
	}

	public Date getTimestamp() {
		return reference.get();
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public Object clone() {
		MartiniPreProcessor processor = new MartiniPreProcessor(reference);
		for (PropertyIterator i = propertyIterator(); i.hasNext();) {
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
		variables.putObject(CONTEXT_KEY, this);
	}

	@Override
	public void testStarted() {
		synchronized (reference) {
			if (null == reference.get()) {
				Date timestamp = new Date();
				reference.set(timestamp);
			}
		}
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
