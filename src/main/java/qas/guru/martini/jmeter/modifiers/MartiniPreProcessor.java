package qas.guru.martini.jmeter.modifiers;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.gui.MainFrame;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import guru.qas.martini.Mixologist;
import guru.qas.martini.event.MartiniEventPublisher;
import qas.guru.martini.event.SuiteEvent;

import static com.google.common.base.Preconditions.checkState;
import static qas.guru.martini.jmeter.modifiers.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniPreProcessor extends AbstractTestElement implements PreProcessor, TestStateListener {

	protected static final Logger LOG = LoggingManager.getLoggerFor(MartiniPreProcessor.class.getName());
	protected static final Map<String, ClassPathXmlApplicationContext> SPRING_CONTEXTS = new HashMap<>();
	protected static final String DEFAULT_SPRING_CONTEXT = "applicationContext.xml";
	protected static final String PROPERTY_KEY_SPRING_CONTEXT_ID = "martini_spring_context_id";

	public MartiniPreProcessor() {
		super();
		super.setProperty(PROPERTY_KEY_SPRING_CONFIGURATION, DEFAULT_SPRING_CONTEXT);
	}

	@Override
	public void process() {
		JMeterVariables variables = getJMeterVariables();
		setMixologist(variables);
		setEventPublisher(variables);
	}

	protected void setMixologist(JMeterVariables variables) {
		Object object = variables.getObject(VALUE_KEY_MIXOLOGIST);
		if (null == object) {
			Mixologist mixologist = getMixologist();
			variables.putObject(VALUE_KEY_MIXOLOGIST, mixologist);
		}
	}

	protected JMeterVariables getJMeterVariables() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == variables) {
			variables = new JMeterVariables();
			threadContext.setVariables(variables);
		}
		return variables;
	}

	protected Mixologist getMixologist() {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		return applicationContext.getBean(Mixologist.class);
	}

	protected void setEventPublisher(JMeterVariables variables) {
		Object object = variables.getObject(VALUE_KEY_EVENT_PUBLISHER);
		if (null == object) {
			MartiniEventPublisher publisher = getPublisher();
			variables.putObject(VALUE_KEY_EVENT_PUBLISHER, publisher);
		}
	}

	protected MartiniEventPublisher getPublisher() {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		return applicationContext.getBean(MartiniEventPublisher.class);
	}

	@Override
	public void testStarted() {
		try {
			startSpring();
			publishStarted();
		}
		catch (Exception e) {
			String message = "Unable to initialize Spring ApplicationContext; halting execution.";
			LOG.error(message, e);
			displayError(message + "\nSee log for details.");
			StandardJMeterEngine.stopEngineNow();
		}
	}

	protected void startSpring() {
		String configLocation = getConfigLocation();
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(configLocation);
		String id = applicationContext.getId();
		synchronized (SPRING_CONTEXTS) {
			SPRING_CONTEXTS.put(id, applicationContext);
		}
		super.setProperty(PROPERTY_KEY_SPRING_CONTEXT_ID, id);
	}

	protected String getConfigLocation() {
		String property = getPropertyAsString(PROPERTY_KEY_SPRING_CONFIGURATION);
		String trimmed = null == property ? null : property.trim();
		checkState(null != trimmed && !trimmed.isEmpty(), "missing or empty Spring Application Context");
		return trimmed;
	}

	protected void displayError(String message) {
		GuiPackage guiPackage = GuiPackage.getInstance();
		MainFrame component = guiPackage.getMainFrame();
		JOptionPane.showMessageDialog(component, message, "Error", JOptionPane.ERROR_MESSAGE);
	}

	private void publishStarted() {
		MartiniEventPublisher publisher = getPublisher();
		JMeterContext threadContext = getThreadContext();
		publisher.publish(SuiteEvent.getStarted(threadContext));
	}

	@Override
	public void testStarted(String s) {
	}

	@Override
	public void testEnded() {
		publishEnded();
		closeSpring();
	}

	private void closeSpring() {
		ClassPathXmlApplicationContext applicationContext = getApplicationContext();
		super.removeProperty(PROPERTY_KEY_SPRING_CONTEXT_ID);
		if (null != applicationContext) {
			String id = applicationContext.getId();
			synchronized (SPRING_CONTEXTS) {
				SPRING_CONTEXTS.remove(id);
			}
			applicationContext.close();
		}
	}

	private void publishEnded() {
		MartiniEventPublisher publisher = getPublisher();
		JMeterContext threadContext = getThreadContext();
		publisher.publish(SuiteEvent.getEnded(threadContext));
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
