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
import java.net.URL;

import org.springframework.core.io.Resource;

import com.google.gson.stream.JsonWriter;

import gherkin.ast.Feature;

import guru.qas.martini.Martini;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.result.MartiniResult;

@SuppressWarnings("WeakerAccess")
final class FeatureMarshaller {

	protected final JsonWriter writer;
	protected final MartiniResult result;
	protected final Feature feature;

	protected FeatureMarshaller(JsonWriter writer, MartiniResult result, Feature feature) {
		this.writer = writer;
		this.result = result;
		this.feature = feature;
	}

	protected void addName() throws IOException {
		String name = feature.getName();
		writer.name("name").value(null == name ? null : name.trim());
	}

	protected void addDescription() throws IOException {
		String description = feature.getDescription();
		writer.name("description").value(null == description ? null : description.trim());
	}

	protected void addResource() throws IOException {
		String path = getResourcePath();
		writer.name("resource").value(path);
	}

	protected String getResourcePath() throws IOException {
		Martini martini = result.getMartini();
		Recipe recipe = martini.getRecipe();
		Resource resource = recipe.getSource();
		URL url = null == resource ? null : resource.getURL();
		return null == url ? null : url.getPath();
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

		protected FeatureMarshaller build() {
			Feature feature = getFeature();
			return new FeatureMarshaller(writer, result, feature);
		}

		protected Feature getFeature() {
			Martini martini = result.getMartini();
			Recipe recipe = martini.getRecipe();
			return recipe.getFeature();
		}
	}
}
