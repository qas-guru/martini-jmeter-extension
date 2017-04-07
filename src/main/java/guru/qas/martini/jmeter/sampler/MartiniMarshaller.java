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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;


import com.google.gson.stream.JsonWriter;

import gherkin.ast.ScenarioDefinition;
import gherkin.pickles.PickleLocation;
import guru.qas.martini.Martini;
import guru.qas.martini.event.Status;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.tag.MartiniTag;

@SuppressWarnings("WeakerAccess")
final class MartiniMarshaller {

	protected final JsonWriter writer;
	protected final MartiniResult result;
	protected final Martini martini;

	protected MartiniMarshaller(JsonWriter writer, MartiniResult result, Martini martini) {
		this.writer = writer;
		this.result = result;
		this.martini = martini;
	}

	protected void addStartTimestamp() throws IOException {
		Long timestamp = result.getStartTimestamp();
		writer.name("startTimestamp").value(timestamp);
	}

	protected void addEndTimestamp() throws IOException {
		Long timestamp = result.getEndTimestamp();
		writer.name("endTimestamp").value(timestamp);
	}

	protected void addId() throws IOException {
		String id = martini.getId();
		writer.name("id").value(id);
	}

	protected void addThreadGroup() throws IOException {
		String name = result.getThreadGroupName();
		writer.name("threadGroup").value(name);
	}

	protected void addThread() throws IOException {
		String name = result.getThreadName();
		writer.name("thread").value(name);
	}

	protected void addLine() throws IOException {
		Recipe recipe = martini.getRecipe();
		PickleLocation location = recipe.getLocation();
		Integer line = null == location ? null : location.getLine();
		writer.name("line").value(line);
	}

	protected void addName() throws IOException {
		String name = martini.getScenarioName();
		writer.name("name").value(name);
	}

	protected void addDescription() throws IOException {
		Recipe recipe = martini.getRecipe();
		ScenarioDefinition scenarioDefinition = recipe.getScenarioDefinition();
		String description = scenarioDefinition.getDescription();
		writer.name("description").value(null == description ? null : description.trim());
	}

	protected void addCategories() throws IOException {
		writer.name("categories");
		writer.beginArray();
		Set<String> categories = result.getCategorizations();
		if (null != categories) {
			for (String category : categories) {
				writer.value(category);
			}
		}
		writer.endArray();
	}

	protected void addTags() throws IOException {

		writer.name("tags");
		writer.beginArray();
		Collection<MartiniTag> tags = martini.getTags();
		if (null != tags) {
			for (MartiniTag tag : tags) {
				addTag(writer, tag);
			}
		}
		writer.endArray();
	}

	protected void addTag(JsonWriter writer, MartiniTag tag) throws IOException {
		writer.beginObject();
		writer.name("name").value(tag.getName());
		writer.name("argument").value(tag.getArgument());
		writer.endObject();
	}

	protected void addStatus() throws IOException {
		Status status = result.getStatus();
		writer.name("status").value(null == status ? null : status.name());
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected JsonWriter writer;
		protected MartiniResult result;

		protected Builder() {
		}

		protected Builder setJsonWriter(JsonWriter writer) {
			this.writer = writer;
			return this;
		}

		protected Builder setMartiniResult(MartiniResult result) {
			this.result = result;
			return this;
		}

		protected MartiniMarshaller build() {
			return new MartiniMarshaller(writer, result, result.getMartini());
		}
	}
}
