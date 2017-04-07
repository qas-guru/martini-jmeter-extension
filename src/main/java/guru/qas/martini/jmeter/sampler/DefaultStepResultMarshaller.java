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
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import com.google.common.base.Throwables;
import com.google.common.io.BaseEncoding;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import gherkin.ast.Location;
import gherkin.ast.Step;
import guru.qas.martini.event.Status;
import guru.qas.martini.result.StepResult;
import guru.qas.martini.step.StepImplementation;

@SuppressWarnings("WeakerAccess")
final class DefaultStepResultMarshaller implements StepResultMarshaller {

	protected static final DefaultStepResultMarshaller INSTANCE = new DefaultStepResultMarshaller();

	protected Gson gson;

	protected DefaultStepResultMarshaller() {
		init();
	}

	void init() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
	}

	@Override
	public String getJson(StepResult result) throws IOException {
		StringWriter writer = new StringWriter();

		try (JsonWriter jsonWriter = gson.newJsonWriter(writer)) {
			jsonWriter.beginObject();
			jsonWriter.name("step");
			jsonWriter.beginObject();

			Delegate delegate = new Delegate(result, jsonWriter);
			delegate.addKeyword();
			delegate.addText();
			delegate.addLine();
			delegate.addMethod();
			delegate.addStatus();
			delegate.addException();
			delegate.addEmbedded();

			jsonWriter.endObject();
			jsonWriter.endObject();
		}

		return writer.getBuffer().toString();
	}

	private static final class Delegate {

		private final StepResult result;
		private final JsonWriter writer;
		protected Logger logger;
		protected Step step;

		private Delegate(StepResult result, JsonWriter writer) {
			this.result = result;
			this.writer = writer;
			init();
		}

		void init() {
			this.step = result.getStep();
			Class<? extends Delegate> implementation = getClass();
			String category = implementation.getName();
			logger = LoggingManager.getLoggerFor(category);
		}

		void addKeyword() throws IOException {
			String keyword = step.getKeyword();
			writer.name("keyword").value(null == keyword ? null : keyword.trim());
		}

		void addText() throws IOException {
			String text = step.getText();
			writer.name("text").value(null == text ? null : text.trim());
		}

		void addLine() throws IOException {
			Location location = step.getLocation();
			int line = location.getLine();
			writer.name("line").value(line);
		}

		void addStatus() throws IOException {
			Status status = result.getStatus();
			writer.name("status").value(null == status ? null : status.name());
		}

		void addMethod() throws IOException {
			writer.name("method");
			writer.beginObject();

			StepImplementation implementation = result.getStepImplementation();
			Method method = null == implementation ? null : implementation.getMethod();

			addClass(method);
			addName(method);
			addParameters(method);
			addPattern(implementation);
			writer.endObject();
		}

		void addClass(Method method) throws IOException {
			Class<?> clazz = null == method ? null : method.getDeclaringClass();
			writer.name("class").value(null == clazz ? null : clazz.toString());
		}

		void addName(Method method) throws IOException {
			writer.name("name").value(null == method ? null : method.getName());
		}

		void addParameters(Method method) throws IOException {
			writer.name("parameters");
			writer.beginArray();

			Class<?>[] parameterTypes = null == method ? new Class[0] : method.getParameterTypes();
			for (Class c : parameterTypes) {
				writer.value(c.getName());
			}

			writer.endArray();
		}

		void addPattern(StepImplementation implementation) throws IOException {
			Pattern pattern = null == implementation ? null : implementation.getPattern();
			String regex = null == pattern ? null : pattern.pattern();
			writer.name("pattern").value(regex);
		}

		void addEmbedded() throws IOException {
			writer.name("embedded");
			writer.beginArray();

			List<HttpEntity> embedded = result.getEmbedded();
			if (null != embedded) {
				for (HttpEntity entity : embedded) {
					writer.beginObject();
					add(entity.getContentType());
					long contentLength = entity.getContentLength();
					writer.name("Content-Length").value(contentLength);
					add(entity.getContentEncoding());
					addContent(entity);
					writer.endObject();
				}
			}
			writer.endArray();
		}

		void add(Header header) throws IOException {
			if (null != header) {
				String name = header.getName();
				String value = header.getValue();
				writer.name(name).value(value);
			}
		}

		void addContent(HttpEntity entity) throws IOException {

			String content = null;
			try (InputStream in = ByteStreams.limit(entity.getContent(), 0)
			) {
				byte[] bytes = ByteStreams.toByteArray(in);
				BaseEncoding encoding = BaseEncoding.base64();
				content = encoding.encode(bytes);
			}
			catch (Exception e) {
				logger.warn("unable to serialize HttpEntity contents", e);
			}
			writer.name("content").value(content);
		}

		void addException() throws IOException {
			@SuppressWarnings("ThrowableNotThrown") Exception exception = result.getException();
			String stacktrace = null == exception ? null : Throwables.getStackTraceAsString(exception);
			writer.name("exception").value(stacktrace);
		}

	}

	protected static DefaultStepResultMarshaller getInstance() {
		return INSTANCE;
	}
}
