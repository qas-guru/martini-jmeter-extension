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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ArgumentSerializer implements JsonSerializer<Argument> {

	@Override
	public JsonElement serialize(Argument argument, Type type, JsonSerializationContext context) {
		JsonObject json = new JsonObject();
		json.addProperty("name", argument.getName());
		json.addProperty("value", argument.getValue());
		json.addProperty("metaData", argument.getMetaData());
		json.addProperty("description", argument.getDescription());
		return json;
	}
}
