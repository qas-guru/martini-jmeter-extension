package guru.qas.martini.jmeter.preprocessor;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.util.JMeterStopTestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import guru.qas.martini.jmeter.Messages;
import guru.qas.martini.jmeter.Properties;
import guru.qas.martini.jmeter.Variables;

import static org.springframework.core.env.StandardEnvironment.*;

@SuppressWarnings({"unused", "WeakerAccess"}) // Instantiated/called by JMeter.
public class SpringPreProcessor extends AbstractTestElement
	implements Serializable, Cloneable, PreProcessor, TestStateListener, LoopIterationListener, TestBean {

	private static final long serialVersionUID = 3058615270228163387L;

	protected static final Logger LOGGER = LoggerFactory.getLogger(SpringPreProcessor.class);
	protected static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource();

	static {
		MESSAGE_SOURCE.setBasename("SpringPreProcessorBundle");
	}

	// These must match field names exactly.
	protected static final String PROPERTY_ENVIRONMENT_VARIABLES = "environmentVariables";
	protected static final String PROPERTY_SPRING_CONFIG_LOCATIONS = "configurationLocations";

	public static final String VARIABLE_SPRING_CONTEXT = "martini.spring.application.context";

	// Serialized.
	protected List<Argument> environmentVariables;
	protected List<String> configurationLocations;

	// Shared.
	protected transient ClassPathXmlApplicationContext springContext;

	public List<Argument> getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(List<Argument> environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public List<String> getConfigurationLocations() {
		return configurationLocations;
	}

	public void setConfigurationLocations(List<String> configurationLocations) {
		this.configurationLocations = configurationLocations;
	}

	@Override
	public Object clone() {
		SpringPreProcessor clone = (SpringPreProcessor) super.clone();
		clone.springContext = springContext;
		return clone;
	}

	@Override
	public void testStarted() {
		try {
			String[] locations = Properties.toNormalizedStringArray(this, PROPERTY_SPRING_CONFIG_LOCATIONS);
			springContext = getNewSpringContext(locations);
			Variables.put(VARIABLE_SPRING_CONTEXT, springContext);
		}
		catch (Exception e) {
			String message = getMessage("initialization.error");
			LOGGER.error(message, e);
			String title = getMessage("initialization.error.title", getName());
			JMeterUtils.reportErrorToUser(message, title, e);
			throw new JMeterStopTestException(message, e);
		}
	}

	protected static String getMessage(String code, Object... args) {
		return Messages.getMessage(MESSAGE_SOURCE, code, args);
	}

	protected ClassPathXmlApplicationContext getNewSpringContext(String[] locations) {
		ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(locations, false);
		setJMeterEnvironment(springContext);
		springContext.refresh();
		springContext.registerShutdownHook();
		return springContext;
	}

	protected void setJMeterEnvironment(ClassPathXmlApplicationContext springContext) {
		ConfigurableEnvironment environment = springContext.getEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		PropertySource<?> systemSource = propertySources.get(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		ArgumentListPropertySource propertySource = getJMeterPropertySource();
		propertySources.addAfter(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, propertySource);
	}

	protected ArgumentListPropertySource getJMeterPropertySource() {
		String name = super.getName();
		return ArgumentListPropertySource.builder().setName(name).setArguments(environmentVariables).build();
	}

	@Override
	public void testStarted(String s) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		Variables.put(VARIABLE_SPRING_CONTEXT, springContext);
	}

	@Override
	public void process() {
	}

	public static void autowire(@Nullable Object bean) {
		if (null != bean) {
			Class<?> implementation = bean.getClass();
			Configurable configurable = implementation.getDeclaredAnnotation(Configurable.class);
			if (null != configurable) {
				ApplicationContext springContext = Variables.getRequired(VARIABLE_SPRING_CONTEXT, ApplicationContext.class);
				AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
				beanFactory.autowireBean(bean);
			}
		}
	}

	public static void initialize(@Nullable Object bean) {
		if (InitializingBean.class.isInstance(bean)) {
			ApplicationContext springContext = Variables.getRequired(VARIABLE_SPRING_CONTEXT, ApplicationContext.class);
			AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
			beanFactory.initializeBean(bean, bean.getClass().getName());
		}
	}

	@Override
	public void testEnded(String s) {
		testEnded();
	}

	@Override
	public void testEnded() {
		synchronized (SpringPreProcessor.class) {
			if (null != springContext && springContext.isRunning()) {
				springContext.stop();
			}
		}
		springContext = null;
	}
}