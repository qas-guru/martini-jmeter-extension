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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Stack;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("WeakerAccess")
public class Meh {

	private static final Logger LOGGER = LoggerFactory.getLogger(Meh.class);

	protected final XMLStreamReader reader;
	protected final Stack<Sample.Builder> stack;

	protected Meh(XMLStreamReader reader) {
		this.reader = reader;
		this.stack = new Stack<>();
	}

	public void doSomething() throws XMLStreamException {
		while (reader.hasNext()) {
			int next = reader.next();
			int event = reader.getEventType();
			switch (event) {
				case XMLStreamConstants.START_ELEMENT:
					handleStartElement();
					break;
				case XMLStreamConstants.CHARACTERS:
					break;
				case XMLStreamConstants.END_ELEMENT:
					handleEndElement();
					break;
			}
		}
	}

	protected void handleEndElement() {
		String localName = reader.getLocalName();
		if (localName.equals("sample")) {
			Sample.Builder builder = stack.pop();
			Sample sample = builder.build();

			if (!stack.isEmpty()) {
				Sample.Builder parent = stack.peek();
				parent.addSub(sample);
			} else {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String json = gson.toJson(sample);
				// TODO: better handling of json
				System.out.println(json);
			}
		}
	}

	protected void handleStartElement() throws XMLStreamException {
		String name = reader.getLocalName();
		switch (name) {
			case "testResults":
				handleTestResults();
				break;
			case "sample":
				handleSample();
				break;
			case "responseData":
				handleResponseData();
				break;
			default:
				handleOtherElement(name);
		}

	}

	protected void handleTestResults() {
		String version = reader.getAttributeValue(null, "version");
		if (null == version || !version.equals("1.2")) {
			LOGGER.warn("testResults version is {}, this class expectes to work with version 1.2", version);
		}
	}

	protected void handleOtherElement(String name) {
		System.out.println("element localname: " + name);
		int attributeCount = reader.getAttributeCount();
		for (int i = 0; i < attributeCount; i++) {
			String attributeLocalName = reader.getAttributeLocalName(i);
			System.out.println("attribute localname: " + attributeLocalName);
			String attributeValue = reader.getAttributeValue(i);
			System.out.println("attribute value: " + attributeValue);
		}
	}

	protected void handleSample() {
		String timeElapsed = reader.getAttributeValue(null, "t");// time elapsed in ms
		String timestamp = reader.getAttributeValue(null, "ts"); // timestamp
		Sample.Builder builder = Sample.builder().setTimeElapsed(timeElapsed).setTimestamp(timestamp);
		stack.push(builder);
	}

	protected void handleResponseData() throws XMLStreamException {
		String elementText = reader.getElementText();

		Object peek = stack.peek();
		checkState(Sample.Builder.class.isInstance(peek), "expected a Sample.Builder on the stack but found %s", peek);
		Sample.Builder builder = Sample.Builder.class.cast(peek);
		builder.setResponseData(elementText);
	}

	public static Builder builder() {
		return new Builder();
	}

@SuppressWarnings("WeakerAccess")
public static class Builder {

	protected Builder() {
	}

	public Meh build(String path) throws FileNotFoundException, XMLStreamException {
		File file = new File(path);
		FileInputStream in = new FileInputStream(file);
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		XMLStreamReader xmlStreamReader = xmlInputFactory.createXMLStreamReader(in);
		return new Meh(xmlStreamReader);
	}

}

	/*
	ATTRIBUTE
	CDATA
	CHARACTERS
	COMMENT
	DTD
	END_DOCUMENT
	END_ELEMENT
	ENTITY_DECLARATION
	ENTITY_REFERENCE
	NAMESPACE
	NOTATION_DECLARATION
	PROCESSING_INSTRUCTION
	SPACE
	START_DOCUMENT
	START_ELEMENT
	 */
	public static void main(String[] args) throws XMLStreamException, FileNotFoundException {

		checkState(1 == args.length, "specify a single argument filename");
		String path = args[0];
		Meh application = Meh.builder().build(path);
		application.doSomething();
	}
}
