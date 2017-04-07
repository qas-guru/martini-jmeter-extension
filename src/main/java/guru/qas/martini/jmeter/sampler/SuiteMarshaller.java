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
import java.util.UUID;

import com.google.gson.stream.JsonWriter;

import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.result.MartiniResult;

@SuppressWarnings("WeakerAccess")
final class SuiteMarshaller {

	protected final JsonWriter writer;
	protected final SuiteIdentifier identifier;

	protected SuiteMarshaller(JsonWriter writer, SuiteIdentifier identifier) {
		this.writer = writer;
		this.identifier = identifier;
	}

	protected void addId() throws IOException {
		UUID uuid = null == identifier ? null : identifier.getId();
		String id = null == uuid ? null : uuid.toString();
		writer.name("id").value(id);
	}

	protected void addName() throws IOException {
		String name = identifier.getName();
		writer.name("name").value(name);
	}

	protected void addHost() throws IOException {
		writer.name("host");
		writer.beginObject();
		addHostname();
		addHostAddress();
		writer.endObject();
	}

	protected void addHostname() throws IOException {
		String name = identifier.getHostname();
		writer.name("name").value(name);
	}

	protected void addHostAddress() throws IOException {
		String ip = identifier.getHostAddress();
		writer.name("ip").value(ip);
	}


	protected static Builder builder() {
		return new Builder();
	}

	protected static class Builder {

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

		protected SuiteMarshaller build() {
			SuiteIdentifier identifier = result.getSuiteIdentifier();
			return new SuiteMarshaller(writer, identifier);
		}
	}
}
