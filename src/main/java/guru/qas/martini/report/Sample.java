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

package guru.qas.martini.report;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@SuppressWarnings("WeakerAccess")
public class Sample {

	protected final Gson gson;
	protected final String json;
	protected final ImmutableList<Sample> subSamples;

	protected Sample(Gson gson, String json, ImmutableList<Sample> subSamples) {
		this.gson = gson;
		this.json = json;
		this.subSamples = subSamples;
	}

	protected String getJson() {
		JsonObject container = gson.fromJson(json, JsonObject.class);
		if (container.has("martini")) {
			JsonElement element = container.get("martini");
			JsonObject object = element.getAsJsonObject();
			JsonArray stepArray = getStepArray();
			object.add("steps", stepArray);
		} else if (container.has("step")) {
			container = container.get("step").getAsJsonObject();
		}
		return gson.toJson(container);

	}

	protected JsonArray getStepArray() {
		JsonArray stepArray = new JsonArray();
		for (Sample sample : subSamples) {
			String json = sample.getJson();
			JsonObject object = gson.fromJson(json, JsonObject.class);
			stepArray.add(object);
		}
		return stepArray;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("json", json)
			.add("subSamples", Joiner.on(",").skipNulls().join(subSamples))
			.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		protected static final Gson GSON =  new GsonBuilder().setPrettyPrinting().create();

		protected String json;
		protected List<Sample> subSamples;

		protected Builder() {
			this.subSamples = Lists.newArrayList();
		}

		public Builder setResponseData(String s) {
			this.json = s;
			return this;
		}

		public Builder addSub(Sample sample) {
			subSamples.add(sample);
			return this;
		}

		public Sample build() {
			return new Sample(GSON, json, ImmutableList.copyOf(subSamples));
		}
	}
}
