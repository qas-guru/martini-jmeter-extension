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

package guru.qas.martini.jmeter.spring;

import java.io.StringWriter;
import java.util.HashSet;

import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import guru.qas.martini.gherkin.FeatureWrapper;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.runtime.event.json.MartiniResultSerializer;
import guru.qas.martini.runtime.event.json.StepImplementationSerializer;
import guru.qas.martini.runtime.event.json.StepResultSerializer;
import guru.qas.martini.step.StepImplementation;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings({"WeakerAccess", "unused"})
@Configurable
public class DefaultMartiniResultMarshaller implements MartiniResultMarshaller, InitializingBean {

	protected final Logger logger;

	protected MartiniResultSerializer martiniResultSerializer;
	protected StepResultSerializer stepResultSerializer;
	protected StepImplementationSerializer stepImplementationSerializer;
	protected Gson gson;

	protected HashSet<FeatureWrapper> serializedFeatures;

	@Autowired
	protected void setMartiniResultSerializer(MartiniResultSerializer s) {
		this.martiniResultSerializer = s;
	}

	@Autowired
	protected void setStepResultSerializer(StepResultSerializer s) {
		this.stepResultSerializer = s;
	}

	@Autowired
	protected void setStepImplementationSerializer(StepImplementationSerializer s) {
		this.stepImplementationSerializer = s;
	}

	public DefaultMartiniResultMarshaller() {
		this.logger = LoggerFactory.getLogger(this.getClass());
		serializedFeatures = new HashSet<>();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		serializedFeatures.clear();

		GsonBuilder builder = getGsonBuilder();
		registerTypeAdapters(builder);
		gson = builder.create();
	}

	protected GsonBuilder getGsonBuilder() {
		return new GsonBuilder()
			.setPrettyPrinting()
			.setLenient()
			.serializeNulls();
	}

	protected void registerTypeAdapters(GsonBuilder builder) {
		builder.registerTypeAdapter(MartiniResult.class, martiniResultSerializer);
		builder.registerTypeAdapter(StepResult.class, stepResultSerializer);
		builder.registerTypeAdapter(StepImplementation.class, stepImplementationSerializer);
	}


	@Override
	public void setJsonResponse(SampleResult sample, MartiniResult result) {
		checkNotNull(sample, "null SampleResult");
		checkNotNull(result, "null MartiniResult");

		try (StringWriter writer = new StringWriter();
			 JsonWriter jsonWriter = gson.newJsonWriter(writer)) {
			gson.toJson(result, MartiniResult.class, jsonWriter);
			jsonWriter.flush();
			String data = writer.toString();
			sample.setResponseData(data, null); // Uses platform-dependent encoding.
		}
		catch (Exception e) {
			logger.warn("unable to set JSON on SampleResult", e);
		}
	}
}

