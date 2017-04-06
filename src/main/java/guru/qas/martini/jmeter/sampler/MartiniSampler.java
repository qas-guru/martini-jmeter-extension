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

import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JMeterStopThreadException;
import org.apache.log.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import gherkin.ast.Feature;
import gherkin.ast.Location;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;
import guru.qas.martini.Martini;
import guru.qas.martini.event.EventManager;
import guru.qas.martini.event.MartiniSuiteIdentifier;
import guru.qas.martini.event.Status;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.DefaultStepResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.Categories;
import guru.qas.martini.tag.MartiniTag;

import static com.google.common.base.Preconditions.checkState;
import static guru.qas.martini.MartiniConstants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler {

	private static final long serialVersionUID = -5644094193554791266L;

	protected transient Logger logger;

	public MartiniSampler() {
		super();
		init();
	}

	protected void init() {
		Class<? extends MartiniSampler> implementation = getClass();
		String category = implementation.getName();
		logger = LoggingManager.getLoggerFor(category);
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
		MartiniSuiteIdentifier suiteIdentifier = getSuiteIdentifier();

		return DefaultMartiniResult.builder()
			.setMartiniSuiteIdentifier(suiteIdentifier)
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

	protected MartiniSuiteIdentifier getSuiteIdentifier() {
		ApplicationContext context = this.getApplicationContext();
		return context.getBean(MartiniSuiteIdentifier.class);
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
		sampleResult.setSampleLabel(null == martini ? "UNKNOWN" : getLabel(martini));
		sampleResult.setSuccessful(false);

		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		samplerContext.put("status", Status.FAILED);
		samplerContext.put("exception", e);

		return sampleResult;
	}

	protected String getLabel(Martini martini) {
		Recipe recipe = martini.getRecipe();

		Feature feature = recipe.getFeature();
		String featureName = feature.getName();

		Pickle pickle = recipe.getPickle();
		String pickleName = pickle.getName();
		return String.format("%s (%s)", pickleName, featureName);
	}

	protected SampleResult sample(DefaultMartiniResult martiniResult) {
		Martini martini = martiniResult.getMartini();
		Map<Step, StepImplementation> stepIndex = martini.getStepIndex();

		String label = getLabel(martini);
		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(label);
		sampleResult.setSuccessful(true);
		sampleResult.sampleStart();

		for (Map.Entry<Step, StepImplementation> mapEntry : stepIndex.entrySet()) {
			Step step = mapEntry.getKey();
			DefaultStepResult stepResult = new DefaultStepResult(step);
			martiniResult.add(stepResult);
			publishBeforeStep(martiniResult);

			StepImplementation implementation = mapEntry.getValue();

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
				StringWriter writer = new StringWriter();
				try (JsonWriter jsonWriter = new JsonWriter(writer)) {
					jsonWriter.setHtmlSafe(true);
					jsonWriter.setLenient(true);
					jsonWriter.setSerializeNulls(true);
					jsonWriter.beginObject();

					String keyword = implementation.getKeyword();
					jsonWriter.name("keyword").value(keyword);

					String text = step.getText();
					jsonWriter.name("text").value(text);

					Location location = step.getLocation();
					int line = location.getLine();
					jsonWriter.name("line").value(line);

					Method method = implementation.getMethod();
					if (null != method) {
						Class<?> clazz = method.getDeclaringClass();
						String methodName = method.getName();
						Class<?>[] parameterTypes = method.getParameterTypes();
						parameterTypes = null == parameterTypes ? new Class<?>[0] : parameterTypes;
						jsonWriter.name("className").value(clazz.getName());
						jsonWriter.name("methodName").value(methodName);
						jsonWriter.name("parameters").value(Joiner.on(", ").join(parameterTypes));

						Pattern pattern = implementation.getPattern();
						jsonWriter.name("pattern").value(pattern.pattern());
					}

					jsonWriter.name("status").value(stepResult.getStatus().name());
					Exception exception = stepResult.getException();
					String stacktrace = null == exception ? "" : Throwables.getStackTraceAsString(exception);
					jsonWriter.name("exception").value(stacktrace);

					List<HttpEntity> embedded = stepResult.getEmbedded();
					if (null != embedded && !embedded.isEmpty()) {
						jsonWriter.beginArray();

						for (HttpEntity entity : embedded) {

							StringWriter stringWriter = new StringWriter();
							OutputStream outputStream = BaseEncoding.base64().encodingStream(stringWriter);
							entity.writeTo(outputStream);
							stringWriter.flush();
							StringBuffer buffer = stringWriter.getBuffer();

							jsonWriter.beginObject();
							jsonWriter.name("httpEntity").value(buffer.toString());
							jsonWriter.endObject();
						}
						jsonWriter.endArray();
					}
					jsonWriter.endObject();
				}

				String response = writer.toString();
				subResult.setDataType(SampleResult.TEXT);
				subResult.setResponseHeaders("Content-Type: application/json");
				subResult.setResponseData(response, "UTF-8");
			}
			catch (Exception e) {
				logger.error("unable to marshall StepResult", e);
			}

			sampleResult.addSubResult(subResult);
			sampleResult.setSuccessful(sampleResult.isSuccessful() && subResult.isSuccessful());
			publishAfterStep(martiniResult);
		}

		try {
			StringWriter writer = new StringWriter();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();

			try (JsonWriter jsonWriter = gson.newJsonWriter(writer)) {
				jsonWriter.setHtmlSafe(true);
				jsonWriter.setLenient(true);
				jsonWriter.setSerializeNulls(true);
				jsonWriter.beginObject();

				jsonWriter.name("id").value(martini.getId());
				Recipe recipe = martini.getRecipe();

				Resource resource = recipe.getSource();
				URL url = null == resource ? null : resource.getURL();
				String path = null == url ? null : url.getPath();
				jsonWriter.name("resource").value(path);

				Integer i = null == path ? null : path.lastIndexOf('!');
				jsonWriter.name("file").value(null == i || i < 0 ? path : path.substring(i + 1));

				PickleLocation location = recipe.getLocation();
				Integer line = null == location ? null : location.getLine();
				jsonWriter.name("line").value(line);

				ScenarioDefinition scenario = recipe.getScenarioDefinition();
				jsonWriter.name("name").value(scenario.getName());
				jsonWriter.name("description").value(scenario.getDescription().trim());

				jsonWriter.name("categories");
				jsonWriter.beginArray();
				Set<String> categories = martiniResult.getCategorizations();
				if (null != categories) {
					for (String category : categories) {
						jsonWriter.value(category);
					}
				}
				jsonWriter.endArray();

				jsonWriter.name("tags");
				jsonWriter.beginArray();
				Collection<MartiniTag> tags = martini.getTags();
				if (null != tags) {
					for (MartiniTag tag : tags) {
						jsonWriter.beginObject();
						jsonWriter.name("name").value(tag.getName());
						jsonWriter.name("argument").value(tag.getArgument());
						jsonWriter.endObject();
					}
				}
				jsonWriter.endArray();

				jsonWriter.name("feature");
				jsonWriter.beginObject();
				Feature feature = recipe.getFeature();
				jsonWriter.name("name").value(feature.getName());
				jsonWriter.name("description").value(feature.getDescription().trim());
				jsonWriter.endObject();

				long startTime = sampleResult.getStartTime();
				jsonWriter.name("startTimestamp").value(sampleResult.getStartTime());
				jsonWriter.name("startDate").value(new Date(startTime).toString());

				long endTime = sampleResult.getEndTime();
				jsonWriter.name("endTimestamp").value(endTime);
				jsonWriter.name("endDate").value(new Date(endTime).toString());

				jsonWriter.name("timeElapsedMs").value(sampleResult.getTime());

				Status status = martiniResult.getStatus();
				jsonWriter.name("status").value(null == status ? null : status.name());

				jsonWriter.name("suite");
				jsonWriter.beginObject();

				MartiniSuiteIdentifier identifier = martiniResult.getMartiniSuiteIdentifier();
				UUID id = identifier.getId();
				jsonWriter.name("id").value(null == id ? null : id.toString());
				jsonWriter.name("name").value(identifier.getSuiteName());
				long timestamp = identifier.getTimestamp();
				jsonWriter.name("timestamp").value(timestamp);
				jsonWriter.name("date").value(new Date(timestamp).toString());

				jsonWriter.name("host");
				jsonWriter.beginObject();
				jsonWriter.name("name").value(JMeterUtils.getLocalHostName());
				jsonWriter.name("ip").value(JMeterUtils.getLocalHostIP());
				jsonWriter.endObject();

				jsonWriter.name("threadGroup").value(martiniResult.getThreadGroupName());
				jsonWriter.name("thread").value(martiniResult.getThreadName());
				jsonWriter.endObject();

				jsonWriter.name("steps");
				jsonWriter.beginArray();
				jsonWriter.endArray();
				jsonWriter.endObject();
			}
			String response = writer.toString();

			sampleResult.setDataType(SampleResult.TEXT);
			sampleResult.setResponseHeaders("Content-Type: application/json");
			sampleResult.setResponseData(response, "UTF-8");
		}
		catch (Exception e) {
			logger.error("unable to marshall MartiniResult", e);
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
		stepResult.addAll(context.getHttpEntities());
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
