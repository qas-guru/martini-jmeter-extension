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

package guru.qas.martini.jmeter.json;

import java.lang.reflect.Type;

import org.apache.jmeter.config.Argument;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

@SuppressWarnings("WeakerAccess")
public class ArgumentDeserializer implements JsonDeserializer<Argument> {

	@Override
	public Argument deserialize(
		JsonElement element, Type type, JsonDeserializationContext context
	) throws JsonParseException {
		JsonObject jsonObject = element.getAsJsonObject();

		String name = getString(jsonObject, "name");
		String value = getString(jsonObject, "value");
		String metaData = getString(jsonObject, "metaData");
		String description = getString(jsonObject, "description");
		return new Argument(name, value, metaData, description);
	}

	protected String getString(JsonObject jsonObject, String key) {
		JsonPrimitive primitive = jsonObject.getAsJsonPrimitive(key);
		return null == primitive ? null : primitive.getAsString();
	}
}
