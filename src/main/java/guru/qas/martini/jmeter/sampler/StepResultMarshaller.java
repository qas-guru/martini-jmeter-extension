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

package guru.qas.martini.jmeter.sampler;

import com.google.gson.stream.JsonWriter;

import guru.qas.martini.result.StepResult;

@SuppressWarnings("WeakerAccess")
final class StepResultMarshaller {

	protected final JsonWriter writer;
	protected final StepResult result;

	protected StepResultMarshaller(JsonWriter writer, StepResult result) {
		this.writer = writer;
		this.result = result;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected JsonWriter writer;
		protected StepResult result;

		protected Builder() {
		}

		protected Builder setJsonWriter(JsonWriter writer) {
			this.writer = writer;
			return this;
		}

		protected Builder setStepResult(StepResult stepResult) {
			this.result = result;
			return this;
		}

		protected StepResultMarshaller build() {
			return new StepResultMarshaller(writer, result);
		}
	}
}
