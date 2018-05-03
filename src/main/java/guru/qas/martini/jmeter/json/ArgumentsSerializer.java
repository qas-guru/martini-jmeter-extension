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
import org.apache.jmeter.config.Arguments;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ArgumentsSerializer implements JsonSerializer<Arguments> {

	@Override
	public JsonElement serialize(Arguments arguments, Type type, JsonSerializationContext context) {
		JsonArray array = new JsonArray();
		int argumentCount = arguments.getArgumentCount();
		for (int i = 0; i < argumentCount; i++) {
			Argument argument = arguments.getArgument(i);
			JsonElement serialized = context.serialize(argument);
			array.add(serialized);
		}
		return array;
	}
}
