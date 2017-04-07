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
import java.io.StringWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import guru.qas.martini.result.MartiniResult;

@SuppressWarnings("WeakerAccess")
public class DefaultMartiniResultMarshaller implements MartiniResultMarshaller {

	protected static final DefaultMartiniResultMarshaller INSTANCE = new DefaultMartiniResultMarshaller();

	protected final Gson gson;

	protected DefaultMartiniResultMarshaller() {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}

	@Override
	public String getJson(MartiniResult result) throws IOException {
		StringWriter writer = new StringWriter();

		try (JsonWriter jsonWriter = getJsonWriter(writer)) {
			jsonWriter.beginObject();
			jsonWriter.name("martini");
			jsonWriter.beginObject();

			startMartini(jsonWriter, result);
			addFeature(jsonWriter, result);
			addSuite(jsonWriter, result);
			jsonWriter.endObject();
			jsonWriter.endObject();
		}
		return writer.toString();
	}

	protected JsonWriter getJsonWriter(StringWriter writer) throws IOException {
		JsonWriter jsonWriter = gson.newJsonWriter(writer);
		jsonWriter.setHtmlSafe(true);
		jsonWriter.setLenient(true);
		jsonWriter.setSerializeNulls(true);
		return jsonWriter;
	}

	protected void startMartini(JsonWriter writer, MartiniResult result) throws IOException {
		MartiniMarshaller delegate =
			MartiniMarshaller.builder().setJsonWriter(writer).setMartiniResult(result).build();
		delegate.addStartTimestamp();
		delegate.addEndTimestamp();
		delegate.addThreadGroup();
		delegate.addThread();
		delegate.addId();
		delegate.addLine();
		delegate.addName();
		delegate.addDescription();
		delegate.addCategories();
		delegate.addTags();
		delegate.addStatus();
	}

	protected void addFeature(JsonWriter writer, MartiniResult result) throws IOException {
		FeatureMarshaller delegate =
			FeatureMarshaller.builder().setJsonWriter(writer).setMartiniResult(result).build();
		writer.name("feature");
		writer.beginObject();
		delegate.addName();
		delegate.addDescription();
		delegate.addResource();
		writer.endObject();
	}

	protected void addSuite(JsonWriter writer, MartiniResult result) throws IOException {
		SuiteMarshaller delegate = SuiteMarshaller.builder().setJsonWriter(writer).setMartiniResult(result).build();
		writer.name("suite");
		writer.beginObject();
		delegate.addId();
		delegate.addName();
		delegate.addHost();
		writer.endObject();
	}

	public static DefaultMartiniResultMarshaller getInstance() {
		return INSTANCE;
	}
}
