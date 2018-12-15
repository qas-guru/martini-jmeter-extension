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

package guru.qas.martini;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import gherkin.ast.Feature;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import guru.qas.martini.gate.MartiniGate;
import guru.qas.martini.gherkin.FeatureWrapper;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;
import guru.qas.martini.tag.MartiniTag;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class SyntheticMartini implements Martini {

	private final Recipe recipe;

	@Override
	public String getId() {
		return recipe.getId();
	}

	@Override
	public Recipe getRecipe() {
		return recipe;
	}

	@Override
	public String getFeatureName() {
		FeatureWrapper wrapper = recipe.getFeatureWrapper();
		Feature feature = wrapper.getFeature();
		return feature.getName();
	}

	@Override
	public String getScenarioName() {
		ScenarioDefinition definition = recipe.getScenarioDefinition();
		return definition.getName();
	}

	public SyntheticMartini(Recipe recipe) {
		this.recipe = checkNotNull(recipe, "null Recipe");
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
	public int getScenarioLine() {
		return 0;
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
