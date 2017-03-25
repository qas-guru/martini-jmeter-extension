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

package fixture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import guru.qas.martini.annotation.And;
import guru.qas.martini.annotation.Given;
import guru.qas.martini.annotation.Steps;

@SuppressWarnings("WeakerAccess")
@Steps
public class TestScope1 {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestScope1.class);

	@SuppressWarnings("FieldCanBeLocal")
	private final ScenarioScopedBean scopedBean;

	@Autowired
	TestScope1(ScenarioScopedBean scopedBean) {
		this.scopedBean = scopedBean;
	}

	@Given("^a non-scoped condition$")
	public void aNonScopedCondition() throws Throwable {
		int ordinal = scopedBean.getOrdinal();
		System.out.println("executing TestScope1.aNonScopedCondition()");
	}

	@Given("^scope condition \"(.+)\"$")
	@And("^scope condition \"(.+)\"$")
	public void scopeConditionVariable(int anInteger) {
		int ordinal = scopedBean.getOrdinal();
		System.out.println("executing TestScope1.scopeConditionVariable(int) with parameter " + anInteger);
	}
}
