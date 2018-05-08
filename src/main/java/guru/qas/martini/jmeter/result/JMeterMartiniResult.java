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

package guru.qas.martini.jmeter.result;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterThread;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import guru.qas.martini.event.Status;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.result.StepResult;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JMeterMartiniResult implements MartiniResult {

	protected final SuiteIdentifier identifier;
	protected final String threadGroupName;
	protected final String threadName;
	protected final UUID id;
	protected final JMeterMartini martini;
	protected final LinkedHashSet<String> categorizations;
	protected final List<StepResult> stepResults;

	@Override
	public SuiteIdentifier getSuiteIdentifier() {
		return identifier;
	}

	@Override
	public UUID getId() {
		return null;
	}

	@Override
	public String getThreadGroupName() {
		return null;
	}

	@Override
	public String getThreadName() {
		return null;
	}

	@Override
	public Set<String> getCategorizations() {
		return ImmutableSet.copyOf(categorizations);
	}

	@Override
	public List<StepResult> getStepResults() {
		return ImmutableList.copyOf(stepResults);
	}

	protected JMeterMartiniResult(
		SuiteIdentifier identifier,
		JMeterMartini martini,
		String threadGroupName,
		String threadName
	) {
		this.identifier = identifier;
		this.martini = martini;
		this.threadGroupName = threadGroupName;
		this.threadName = threadName;
		id = UUID.randomUUID();
		categorizations = new LinkedHashSet<>();
		stepResults = new ArrayList<>();
	}

	public void addStepResult(StepResult stepResult) {
		stepResults.add(checkNotNull(stepResult, "null StepResult"));
		martini.add(stepResult.getStep(), stepResult.getStepImplementation());
	}

	@Override
	public JMeterMartini getMartini() {
		return martini;
	}

	@Override
	public Exception getException() {
		return stepResults.stream().map(StepResult::getException).filter(Objects::nonNull).findFirst().orElse(null);
	}

	@Override
	public Status getStatus() {
		AtomicInteger failed = new AtomicInteger();
		AtomicInteger skipped = new AtomicInteger();
		AtomicInteger passed = new AtomicInteger();

		stepResults.forEach(r -> {
			Status status = r.getStatus();
			switch (status) {
				case FAILED:
					failed.incrementAndGet();
					break;
				case PASSED:
					passed.incrementAndGet();
					break;
				case SKIPPED:
					skipped.incrementAndGet();
					break;
			}
		});

		Status evaluation = Status.SKIPPED;
		if (failed.get() > 0) {
			evaluation = Status.FAILED;
		}
		else if (skipped.get() > 0) {
			evaluation = Status.SKIPPED;
		}
		else if (passed.get() > 0) {
			evaluation = Status.PASSED;
		}

		return evaluation;
	}

	@Override
	public Long getStartTimestamp() {
		StepResult firstStep = stepResults.isEmpty() ? null : stepResults.get(0);
		return null == firstStep ? null : firstStep.getStartTimestamp();
	}

	@Override
	public Long getEndTimestamp() {
		return stepResults.stream()
			.map(StepResult::getEndTimestamp)
			.filter(Objects::nonNull)
			.max(Ordering.natural())
			.orElse(null);
	}

	@Override
	public Long getExecutionTimeMs() {
		Long start = getStartTimestamp();
		Long end = null == start ? null : getEndTimestamp();
		return null == end ? null : end - start;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected JMeterMartini martini;

		protected Builder() {
		}

		public Builder setJMeterMartini(JMeterMartini m) {
			this.martini = m;
			return this;
		}

		public JMeterMartiniResult build() {
			checkState(null != martini, "JMeterMartini not set");
			SuiteIdentifier suiteIdentifier = getSuiteIdentifier();
			String threadGroupName = getThreadGroupName();
			String threadName = getThreadName();
			return new JMeterMartiniResult(suiteIdentifier, martini, threadGroupName, threadName);
		}

		protected SuiteIdentifier getSuiteIdentifier() {
			ApplicationContext applicationContext = MartiniSpringPreProcessor.getApplicationContext();
			return applicationContext.getBean(SuiteIdentifier.class);
		}

		protected String getThreadGroupName() {
			JMeterContext context = JMeterContextService.getContext();
			AbstractThreadGroup threadGroup = context.getThreadGroup();
			return threadGroup.getName();
		}

		protected String getThreadName() {
			JMeterContext context = JMeterContextService.getContext();
			JMeterThread thread = context.getThread();
			return thread.getThreadName();
		}
	}
}