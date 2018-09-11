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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;

import com.google.common.collect.ImmutableList;

import gherkin.ast.Step;
import guru.qas.martini.event.Status;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.step.StepImplementation;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings({"WeakerAccess", "unused"})
public class JMeterStepResult implements StepResult {

	protected Status status;
	protected Step step;
	protected StepImplementation implementation;
	protected Exception exception;
	protected Long startTimestamp;
	protected Long endTimestamp;
	protected List<HttpEntity> entities;

	public void setStatus(Status status) {
		this.status = status;
	}

	@Override
	public Optional<Status> getStatus() {
		return Optional.ofNullable(status);
	}

	@Override
	public Step getStep() {
		return step;
	}

	@Override
	public StepImplementation getStepImplementation() {
		return implementation;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}

	@Override
	public Optional<Exception> getException() {
		return Optional.ofNullable(exception);
	}

	public void setStartTimestamp(Long startTimestamp) {
		this.startTimestamp = startTimestamp;
	}

	@Override
	public Optional<Long> getStartTimestamp() {
		return Optional.ofNullable(startTimestamp);
	}

	public void setEndTimestamp(Long endTimestamp) {
		this.endTimestamp = endTimestamp;
	}

	@Override
	public Optional<Long> getEndTimestamp() {
		return Optional.ofNullable(endTimestamp);
	}

	@Override
	public List<HttpEntity> getEmbedded() {
		return ImmutableList.copyOf(entities);
	}

	public JMeterStepResult(Step step, StepImplementation implementation) {
		this.step = checkNotNull(step, "null Step");
		this.implementation = checkNotNull(implementation, "null StepImplementation");
		setStatus(Status.SKIPPED);
		entities = new ArrayList<>();
	}

	public void addEmbedded(HttpEntity entity) {
		checkNotNull(entity, "null HttpEntity");
		entities.add(entity);
	}

	@Override
	public Optional<Long> getExecutionTime(TimeUnit unit) {
		Long evaluation = null;
		if (getStartTimestamp().isPresent() && getEndTimestamp().isPresent()) {
			evaluation = getEndTimestamp().get() - getStartTimestamp().get();
		}
		return Optional.ofNullable(evaluation);
	}
}
