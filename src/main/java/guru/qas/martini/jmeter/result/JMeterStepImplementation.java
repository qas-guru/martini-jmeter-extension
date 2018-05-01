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

import java.lang.reflect.Method;
import java.util.regex.Pattern;

import gherkin.ast.Step;
import guru.qas.martini.step.StepImplementation;

@SuppressWarnings({"unused", "WeakerAccess"})
public class JMeterStepImplementation implements StepImplementation {

	protected String keyword;
	protected Pattern pattern;
	protected Method method;

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	@Override
	public String getKeyword() {
		return keyword;
	}

	public void setPattern(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public Pattern getPattern() {
		return pattern;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	@Override
	public Method getMethod() {
		return method;
	}

	public JMeterStepImplementation() {
		setKeyword("JMeter");
	}

	@Override
	public boolean isMatch(Step step) {
		return false;
	}
}
