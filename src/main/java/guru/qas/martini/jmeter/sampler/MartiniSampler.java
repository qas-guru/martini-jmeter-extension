/*
Copyright 2017 Penny Rohr Curich

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package guru.qas.martini.jmeter.sampler;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.ImmutableList;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.EventManager;
import guru.qas.martini.event.Status;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.DefaultStepResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.Categories;

import static com.google.common.base.Preconditions.checkState;
import static guru.qas.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler {

	private static final long serialVersionUID = -5644094193554791266L;

	protected transient Logger logger;
	protected transient MartiniResultMarshaller martiniMarshaller;
	protected transient StepResultMarshaller stepMarshaller;

	public MartiniSampler() {
		super();
		init();
	}

	protected void init() {
		Class<? extends MartiniSampler> implementation = getClass();
		String category = implementation.getName();
		logger = LoggingManager.getLoggerFor(category);
		martiniMarshaller = DefaultMartiniResultMarshaller.getInstance();
		stepMarshaller = DefaultStepResultMarshaller.getInstance();
	}

	@Override
	public SampleResult sample(Entry entry) {

		Martini martini = null;
		DefaultMartiniResult martiniResult = null;
		SampleResult sampleResult;

		try {
			martini = getMartini();
			martiniResult = getMartiniResult(martini);
			publishBeforeScenario(martiniResult);
			sampleResult = sample(martiniResult);
		}
		catch (Exception e) {
			logger.error("unable to execute Martini sample", e);
			sampleResult = getError(martini, e);
		}
		finally {
			publishAfterScenario(martiniResult);
		}

		return sampleResult;
	}

	protected DefaultMartiniResult getMartiniResult(Martini martini) {
		String threadGroupName = getThreadGroupName();
		String threadName = getThreadName();
		Set<String> categorizations = getCategorizations(martini);
		SuiteIdentifier identifier = getSuiteIdentifier();

		return DefaultMartiniResult.builder()
			.setMartiniSuiteIdentifier(identifier)
			.setMartini(martini)
			.setThreadGroupName(threadGroupName)
			.setThreadName(threadName)
			.setCategorizations(categorizations)
			.build();
	}

	protected String getThreadGroupName() {
		JMeterContext threadContext = super.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		return threadGroup.getName();
	}

	protected Set<String> getCategorizations(Martini martini) {
		ApplicationContext applicationContext = this.getApplicationContext();
		Categories categories = applicationContext.getBean(Categories.class);
		return categories.getCategorizations(martini);
	}

	protected SuiteIdentifier getSuiteIdentifier() {
		ApplicationContext context = this.getApplicationContext();
		return context.getBean(SuiteIdentifier.class);
	}

	protected void publishBeforeScenario(MartiniResult result) {
		EventManager eventManager = getEventManager();
		eventManager.publishBeforeScenario(this, result);
	}

	protected EventManager getEventManager() {
		ApplicationContext applicationContext = this.getApplicationContext();
		return applicationContext.getBean(EventManager.class);
	}

	protected void publishAfterScenario(MartiniResult result) {
		if (null != result) {
			EventManager eventManager = getEventManager();
			eventManager.publishAfterScenario(this, result);
		}
	}

	protected ApplicationContext getApplicationContext() {
		JMeterContext threadContext = super.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		JMeterProperty property = threadGroup.getProperty(PROPERTY_SPRING_CONTEXT);
		Object o = null == property ? null : property.getObjectValue();
		if (!ApplicationContext.class.isInstance(o)) {
			throw new JMeterStopThreadException("unable to retrieve Spring ApplicationContext from ThreadGroup");
		}
		return ApplicationContext.class.cast(o);
	}

	protected SampleResult getError(@Nullable Martini martini, Exception e) {
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(null == martini ? "UNKNOWN" : martini.getScenarioName());
		sampleResult.setSuccessful(false);

		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		samplerContext.put("status", Status.FAILED);
		samplerContext.put("exception", e);

		return sampleResult;
	}

	protected SampleResult sample(DefaultMartiniResult martiniResult) {
		Martini martini = martiniResult.getMartini();
		Map<Step, StepImplementation> stepIndex = martini.getStepIndex();

		String label = martini.getScenarioName();
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(label);
		sampleResult.setSuccessful(true);
		sampleResult.sampleStart();

		for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {
			Step step = mapEntry.getKey();
			StepImplementation implementation = mapEntry.getValue();
			DefaultStepResult stepResult = new DefaultStepResult(step, implementation);
			martiniResult.add(stepResult);
			publishBeforeStep(martiniResult);

			SampleResult subResult;
			if (sampleResult.isSuccessful()) {
				JMeterContext threadContext = super.getThreadContext();
				SamplerContext samplerContext = new SamplerContext(threadContext);
				samplerContext.clear();

				subResult = getSubResult(step, implementation);
				update(stepResult, subResult, samplerContext);
			}
			else {
				subResult = getSkipped(step);
				stepResult.setStatus(Status.SKIPPED);
			}

			try {
				String json = stepMarshaller.getJson(stepResult);
				subResult.setDataType(SampleResult.TEXT);
				subResult.setResponseHeaders("Content-Type: application/json");
				subResult.setResponseData(json, "UTF-8");
			}
			catch (Exception e) {
				logger.error("unable to marshall StepResult", e);
			}

			sampleResult.addSubResult(subResult);
			sampleResult.setSuccessful(sampleResult.isSuccessful() && subResult.isSuccessful());
			publishAfterStep(martiniResult);
		}

		try {
			martiniResult.setStartTimestamp(sampleResult.getStartTime());
			martiniResult.setEndTimestamp(sampleResult.getEndTime());
			martiniResult.setExecutionTimeMs(sampleResult.getTime());

			String json = martiniMarshaller.getJson(martiniResult);
			sampleResult.setDataType(SampleResult.TEXT);
			sampleResult.setResponseHeaders("Content-Type: application/json");
			sampleResult.setResponseData(json, "UTF-8");
		}
		catch (Exception e) {
			logger.error("unable to marshall MartiniResult; will not include ResponseData", e);
		}
		return sampleResult;
	}

	/**
	 * Returns Martini stored in variable, or null. This allows listeners to inject changes prior to the
	 * Sampler continuing execution.
	 *
	 * @return currentRef Martini from JMeter variables
	 */
	protected Martini getMartini() {
		JMeterContext threadContext = getThreadContext();
		JMeterThread thread = threadContext.getThread();
		String threadName = thread.getThreadName();
		String key = String.format("martini.%s", threadName);

		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(key);

		if (!Martini.class.isInstance(o)) {
			throw new JMeterStopThreadException("unable to retrieve Martini from JMeterVariables");
		}
		return Martini.class.cast(o);
	}

	protected SampleResult getSubResult(Step step, StepImplementation implementation) {
		Method method = implementation.getMethod();
		return null == method ? getUnimplementedSubResult(step) : getSubResult(step, method, implementation.getPattern());
	}

	protected SampleResult getSubResult(Step step, Method method, Pattern pattern) {
		String label = getLabel(step);
		SampleResult result = new SampleResult();
		result.setSuccessful(true);
		result.sampleStart();

		SamplerContext samplerContext = new SamplerContext(super.getThreadContext());

		try {
			ApplicationContext applicationContext = this.getApplicationContext();
			Parameter[] parameters = method.getParameters();
			Object[] arguments = new Object[parameters.length];

			if (parameters.length > 0) {
				String text = step.getText();
				Matcher matcher = pattern.matcher(text);
				checkState(matcher.find(),
					"unable to locate substitution parameters for pattern %s with input %s", pattern.pattern(), text);

				ConversionService conversionService = applicationContext.getBean(ConversionService.class);

				int groupCount = matcher.groupCount();
				for (int i = 0; i < groupCount; i++) {
					String parameterAsString = matcher.group(i + 1);
					Parameter parameter = parameters[i];
					Class<?> parameterType = parameter.getType();
					Object converted = conversionService.convert(parameterAsString, parameterType);
					arguments[i] = converted;
				}
			}

			samplerContext.setStatus(Status.PASSED);
			Class<?> declaringClass = method.getDeclaringClass();
			Object bean = applicationContext.getBean(declaringClass);
			Object returnValue = method.invoke(bean, arguments);
			if (HttpEntity.class.isInstance(returnValue)) {
				HttpEntity entity = HttpEntity.class.cast(returnValue);
				samplerContext.setHttpEntities(Collections.singleton(entity));
			}
		}
		catch (Exception e) {
			samplerContext.setStatus(Status.FAILED);
			samplerContext.setException(e);
			result.setSuccessful(false);
			label = "FAIL: " + label;
		}
		finally {
			result.sampleEnd();
			result.setSampleLabel(label);
		}
		return result;
	}

	protected SampleResult getUnimplementedSubResult(Step step) {
		SamplerContext samplerContext = new SamplerContext(super.getThreadContext());
		samplerContext.setStatus(Status.SKIPPED);

		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String label = getLabel(step);
		result.setSampleLabel("UNIMPLEMENTED: " + label);
		return result;
	}

	protected String getLabel(Step step) {
		String keyword = step.getKeyword();
		String normalizedKeyword = null == keyword ? "" : keyword.trim();
		String text = step.getText();
		return normalizedKeyword.isEmpty() ? text : String.format("%s %s", normalizedKeyword, text);
	}

	protected void publishBeforeStep(MartiniResult result) {
		EventManager eventManager = getEventManager();
		eventManager.publishBeforeStep(this, result);
	}

	protected void publishAfterStep(MartiniResult result) {
		EventManager eventManager = getEventManager();
		eventManager.publishAfterStep(this, result);
	}

	protected void update(DefaultStepResult stepResult, SampleResult result, SamplerContext context) {
		long startTime = result.getStartTime();
		stepResult.setStartTimestamp(startTime);

		long endTime = result.getEndTime();
		stepResult.setEndTimestamp(endTime);

		stepResult.setException(context.getException());
		stepResult.setStatus(context.getStatus());
		ImmutableList<HttpEntity> httpEntities = context.getHttpEntities();
		if (null != httpEntities) {
			for (HttpEntity entity : httpEntities) {
				stepResult.add(entity);
			}
		}
	}

	protected SampleResult getSkipped(Step step) {
		SamplerContext samplerContext = new SamplerContext(super.getThreadContext());
		samplerContext.setStatus(Status.SKIPPED);

		SampleResult result = new SampleResult();
		result.setSuccessful(false);
		String keyword = step.getKeyword();
		String trimmedKeyword = null == keyword ? "" : keyword.trim();
		String text = step.getText();
		String label = String.format("SKIPPED: %s %s", trimmedKeyword, text);
		result.setSampleLabel(label);
		return result;
	}
}
