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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.MartiniTag;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class JMeterMartini implements Martini {

	protected final JMeterRecipe recipe;
	protected final String featureName;
	protected final String scenarioName;
	protected final LinkedHashSet<MartiniTag> tags;
	protected final LinkedHashMap<Step, StepImplementation> stepIndex;

	@Override
	public JMeterRecipe getRecipe() {
		return recipe;
	}

	@Override
	public String getFeatureName() {
		return featureName;
	}

	@Override
	public String getScenarioName() {
		return scenarioName;
	}

	@Override
	public int getScenarioLine() {
		return 0;
	}

	@Override
	public Collection<MartiniTag> getTags() {
		return ImmutableSet.copyOf(tags);
	}

	@Override
	public Map<Step, StepImplementation> getStepIndex() {
		return ImmutableMap.copyOf(stepIndex);
	}

	protected JMeterMartini(JMeterRecipe recipe, String featureName, String scenarioName) {
		this.recipe = recipe;
		this.featureName = featureName;
		this.scenarioName = scenarioName;
		this.tags = new LinkedHashSet<>();
		this.stepIndex = new LinkedHashMap<>();
	}

	public void add(Step step, StepImplementation stepImplementation) {
		checkNotNull(step, "null Step");
		checkNotNull(stepImplementation, "null StepImplementation");
		stepIndex.put(step, stepImplementation);
	}

	@Override
	public String getId() {
		String normalizedFeatureName = getNormalized(getFeatureName());
		String normalizedScenarioName = getNormalized(getScenarioName());
		int line = getScenarioLine();
		return String.format("%s:%s:%s", normalizedFeatureName, normalizedScenarioName, line);
	}

	protected String getNormalized(String s) {
		String trimmed = null == s ? "" : s.replaceAll("\\s+", " ").trim();
		return trimmed.isEmpty() ? "unknown" : trimmed.replaceAll("\\W+", "_");
	}

	@Override
	public boolean isAnyStepAnnotated(Class<? extends Annotation> implementation) {
		StepImplementation match = getStepIndex().values().stream()
			.filter(Objects::nonNull)
			.filter(i -> {
				Class c = i.getClass();
				Annotation[] annotations = c.getAnnotationsByType(implementation);
				return annotations.length > 0;
			})
			.findAny()
			.orElse(null);
		return null != match;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected String featureName;
		protected String scenarioName;

		protected Builder() {
		}

		public Builder setFeatureName(String s) {
			this.featureName = s;
			return this;
		}

		public Builder setScenarioName(String s) {
			this.scenarioName = s;
			return this;
		}

		public JMeterMartini build() {
			JMeterRecipe recipe = new JMeterRecipe();
			return new JMeterMartini(recipe, featureName, scenarioName);
		}
	}
}
