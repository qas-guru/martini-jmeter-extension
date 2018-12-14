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

package guru.qas.martini.steps;

import guru.qas.martini.annotation.Given;
import guru.qas.martini.annotation.Steps;
import guru.qas.martini.annotation.Then;
import guru.qas.martini.annotation.When;

@Steps
public class TestSteps {

	public TestSteps() {
		System.out.println("TEST STEPS INSTANTIATED");
	}

	@Given("^a situation$")
	public void givenASituation() {
	}

	@When("^something happens$")
	public void whenSomethingHappens() {
	}

	@Then("^an outcome is expected$")
	public void thenAnOutcomeIsExpected() {
	}
}
