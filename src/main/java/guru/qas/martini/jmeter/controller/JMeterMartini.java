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

package guru.qas.martini.jmeter.controller;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.gate.MartiniGate;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.MartiniTag;

public class JMeterMartini implements Martini {

	@Override
	public String getId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Recipe getRecipe() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<Step, StepImplementation> getStepIndex() {
		return ImmutableMap.of();
	}

	@Override
	public Collection<MartiniGate> getGates() {
		return ImmutableList.of();
	}

	@Override
	public Collection<MartiniTag> getTags() {
		return ImmutableList.of();
	}

	@Override
	public String getFeatureName() {
		JMeterContext context = JMeterContextService.getContext();
		AbstractThreadGroup threadGroup = context.getThreadGroup();
		return null == threadGroup ? "" : threadGroup.getName();
	}

	@Override
	public String getScenarioName() {
		JMeterContext context = JMeterContextService.getContext();
		return context.getThread().getThreadName();
	}

	@Override
	public int getScenarioLine() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T extends Annotation> List<T> getStepAnnotations(Class<T> implementation) {
		return ImmutableList.of();
	}

	@Override
	public boolean isAnyStepAnnotated(Class<? extends Annotation> implementation) {
		return false;
	}
}
