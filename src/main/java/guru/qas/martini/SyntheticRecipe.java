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

import gherkin.ast.Background;
import gherkin.ast.ScenarioDefinition;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;
import guru.qas.martini.gherkin.FeatureWrapper;
import guru.qas.martini.gherkin.Recipe;

public class SyntheticRecipe implements Recipe {

	@Override
	public FeatureWrapper getFeatureWrapper() {
		return null;
	}

	@Override
	public Pickle getPickle() {
		return null;
	}

	@Override
	public PickleLocation getLocation() {
		return null;
	}

	@Override
	public ScenarioDefinition getScenarioDefinition() {
		return null;
	}

	@Override
	public Background getBackground() {
		return null;
	}

	@Override
	public String getId() {
		return null;
	}
}
