package qas.guru.martini.jmeter.modifiers;

import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import guru.qas.martini.Mixologist;

import static qas.guru.martini.jmeter.modifiers.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	protected static final Logger LOG = LoggingManager.getLoggerForClass();
	private static final Map<String, ClassPathXmlApplicationContext> SPRING_CONTEXTS = new HashMap<>();
	public static final String DEFAULT_SPRING_CONTEXT = "applicationContext.xml";

	public MartiniPreProcessor() {
		super();
		super.setProperty(PROPERTY_KEY_SPRING_CONFIGURATION, DEFAULT_SPRING_CONTEXT);
	}

	@Override
	public void process() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}

		Object object = variables.getObject(VALUE_KEY_MIXOLOGIST);
		if (null == object) {
			Mixologist mixologist = getMixologist();
			variables.putObject(VALUE_KEY_MIXOLOGIST, mixologist);
		}
	}

	protected Mixologist getMixologist() {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		if (null == applicationContext) {
			LOG.warn("Spring application context not initialized");
		}
		return null == applicationContext ? null : applicationContext.getBean(Mixologist.class);
	}

	@Override
	public void testStarted() {
		String config = super.getPropertyAsString(PROPERTY_KEY_SPRING_CONFIGURATION);
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(config);
		String id = applicationContext.getId();
		synchronized (SPRING_CONTEXTS) {
			SPRING_CONTEXTS.put(id, applicationContext);
		}
		super.setProperty(PROPERTY_KEY_SPRING_CONTEXT_ID, id);
	}

	@Override
	public void testStarted(String s) {
	}

	@Override
	public void testEnded() {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		super.removeProperty(PROPERTY_KEY_SPRING_CONTEXT_ID);
		if (null != applicationContext) {
			String id = applicationContext.getId();
			synchronized(SPRING_CONTEXTS) {
				SPRING_CONTEXTS.remove(id);
			}
			applicationContext.close();
		}
	}

	private ClassPathXmlApplicationContext getApplicationContext() {
		String id = super.getPropertyAsString(PROPERTY_KEY_SPRING_CONTEXT_ID);

		ClassPathXmlApplicationContext context = null;
		if (null != id) {
			synchronized (SPRING_CONTEXTS) {
				context = SPRING_CONTEXTS.get(id);
			}
		}
		return context;
	}

	@Override
	public void testEnded(String s) {
	}
}
