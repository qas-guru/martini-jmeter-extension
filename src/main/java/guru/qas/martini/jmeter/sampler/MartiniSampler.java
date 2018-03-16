/*
Copyright 2018 Penny Rohr Curich

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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.event.Status;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.jmeter.Constants;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.runtime.harness.MartiniCallable;

import static com.google.common.base.Preconditions.checkState;
import static guru.qas.martini.jmeter.Constants.*;

@SuppressWarnings("WeakerAccess")
public class MartiniSampler extends AbstractSampler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniSampler.class);

	@Override
	public SampleResult sample(Entry e) {

		SampleResult result = initializeSampleResult();
		result.sampleStart();

		try {
			Martini martini = getFromSamplerContext(Martini.class, KEY_CURRENT_MARTINI);
			setSampleLabel(result, martini);

			MartiniCallable callable = getCallable(martini);
			MartiniResult martiniResult = callable.call();

			List<StepResult> stepResults = martiniResult.getStepResults();
			stepResults.forEach(r -> populate(result, r));
			setStatus(result, martiniResult);
		}
		catch (Exception exception) {
			String stackTrace = Throwables.getStackTraceAsString(exception);
			result.setResponseMessage(stackTrace);
			result.setSuccessful(false);
		}
		finally {
			setSampleEnd(result);
			Map<String, Object> samplerContext = getSamplerContext();
			samplerContext.remove(Constants.KEY_CURRENT_MARTINI);
		}
		return result;
	}

	protected SampleResult initializeSampleResult() {
		SampleResult result = new SampleResult();
		result.setDataType(SampleResult.TEXT);
		String name = getName();
		result.setSampleLabel(name);
		return result;
	}

	protected ApplicationContext getSpringContext() {
		return getFromSamplerContext(ApplicationContext.class, KEY_SPRING_CONTEXT);
	}

	protected void setSampleLabel(SampleResult result, Martini martini) {
		String scenarioName = martini.getScenarioName();
		String sampleLabel = String.format("%s: %s", result.getSampleLabel(), scenarioName);
		result.setSampleLabel(sampleLabel);
	}

	protected MartiniCallable getCallable(Martini martini) {
		SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
		MartiniCallable callable = new MartiniCallable(suiteIdentifier, martini);
		AutowireCapableBeanFactory beanFactory = getBeanFactory();
		beanFactory.autowireBean(callable);
		return callable;
	}

	protected AutowireCapableBeanFactory getBeanFactory() {
		ApplicationContext springContext = getSpringContext();
		return springContext.getAutowireCapableBeanFactory();
	}

	protected SuiteIdentifier getSuiteIdentifier() {
		ApplicationContext springContext = getSpringContext();
		return springContext.getBean(SuiteIdentifier.class);
	}

	protected void populate(SampleResult result, StepResult r) {
		SampleResult subResult = new SampleResult();
		result.addSubResult(subResult);
		setSampleLabel(subResult, r);
		setElapsed(subResult, r);
		setStatus(subResult, r);
		setException(subResult, r);
		setHttpEntities(subResult, r);
	}

	protected void setSampleLabel(SampleResult result, StepResult r) {
		Step step = r.getStep();
		String keyword = step.getKeyword();
		String text = step.getText();
		String label = String.format("%s %s", keyword, text);
		result.setSampleLabel(label);
	}

	protected void setElapsed(SampleResult result, StepResult r) {
		Long startTimestamp = r.getStartTimestamp();
		Long executionTime = r.getExecutionTime(TimeUnit.MILLISECONDS);
		if (null != startTimestamp) {
			result.setStampAndTime(startTimestamp, null == executionTime ? 0 : executionTime);
		}
	}

	protected void setStatus(SampleResult result, StepResult r) {
		Status status = r.getStatus();
		result.setSuccessful(Status.PASSED == status);
	}

	protected void setStatus(SampleResult result, MartiniResult martiniResult) {
		Status status = martiniResult.getStatus();
		result.setSuccessful(Status.PASSED == status);
	}

	protected void setException(SampleResult result, StepResult r) {
		Exception exception = r.getException();
		if (null != exception) {
			AssertionResult assertionResult = new AssertionResult("Exception");
			assertionResult.setError(false);
			assertionResult.setFailure(true);
			String stackTrace = Throwables.getStackTraceAsString(exception);
			assertionResult.setFailureMessage(stackTrace);
			result.addAssertionResult(assertionResult);
		}
	}

	protected void setHttpEntities(SampleResult result, StepResult r) {
		List<HttpEntity> entities = r.getEmbedded();
		if (null != entities && !entities.isEmpty()) {
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			AtomicInteger i = new AtomicInteger(0);
			entities.forEach(e -> addEntity(builder, i.incrementAndGet(), e));
			HttpEntity multipartEntity = builder.build();
			setResponseData(result, multipartEntity);
		}
	}

	protected void addEntity(MultipartEntityBuilder builder, int ordinal, HttpEntity entity) {
		String name = String.format("HttpEntity-%s", ordinal);
		Header header = entity.getContentType();
		String headerValue = header.getValue();
		try (InputStream in = entity.getContent()) {
			ContentType contentType = ContentType.parse(headerValue);
			InputStreamBody contentBody = new InputStreamBody(in, contentType);
			builder.addPart(name, contentBody);
		}
		catch (IOException e1) {
			LOGGER.warn("unable to attach HttpEntity to SampleResult", e1);
		}
	}

	protected void setResponseData(SampleResult result, HttpEntity entity) {
		try (InputStream in = entity.getContent()) {
			byte[] bytes = ByteStreams.toByteArray(in);
			result.setResponseData(bytes);
		}
		catch (IOException e) {
			LOGGER.warn("unable to attach HttpEntity to SampleResult", e);
		}
	}

	protected void setSampleEnd(SampleResult result) {
		if (0 == result.getEndTime()) {
			result.sampleEnd();
		}
	}

	protected <T> T getFromSamplerContext(Class<T> implementation, String key) {
		Map<String, Object> samplerContext = getSamplerContext();
		Object o = samplerContext.get(key);
		checkState(implementation.isInstance(o),
			"object of type %s not present in SamplerContext under key %s", implementation.getCanonicalName(), key);
		return implementation.cast(o);
	}

	protected Map<String, Object> getSamplerContext() {
		JMeterContext threadContext = super.getThreadContext();
		return threadContext.getSamplerContext();
	}
}
