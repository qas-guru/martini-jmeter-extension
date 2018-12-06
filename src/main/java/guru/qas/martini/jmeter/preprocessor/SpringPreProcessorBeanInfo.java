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

package guru.qas.martini.jmeter.preprocessor;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.TableEditor;

import static com.google.common.base.Preconditions.checkNotNull;
import static guru.qas.martini.jmeter.preprocessor.SpringPreProcessor.*;

@SuppressWarnings({"unused", "WeakerAccess"}) // Used by bean introspection.
public class SpringPreProcessorBeanInfo extends BeanInfoSupport {

	protected static final String LABEL_OPTIONS = "options.label";
	protected static final String LABEL_NAME = "environment.name.label";
	protected static final String LABEL_VALUE = "environment.value.label";
	protected static final String LABEL_DESCRIPTION = "environment.description.label";
	protected static final String LABEL_PATTERN = "config.location.pattern.label";

	protected Function<String, String> messageFunction;

	public SpringPreProcessorBeanInfo() {
		super(SpringPreProcessor.class);

		messageFunction = getMessageFunction();

		String optionsLabel = getLabel(LABEL_OPTIONS);
		createPropertyGroup(optionsLabel,
			new String[]{PROPERTY_ENVIRONMENT_VARIABLES, PROPERTY_SPRING_CONFIG_LOCATIONS});

		PropertyDescriptor p = property(PROPERTY_ENVIRONMENT_VARIABLES);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, new ArrayList<Argument>());
		p.setPropertyEditorClass(TableEditor.class);
		p.setValue(TableEditor.CLASSNAME, Argument.class.getName());
		p.setValue(TableEditor.OBJECT_PROPERTIES, new String[]{"name", "value", "description"});

		String nameLabel = getLabel(LABEL_NAME);
		String valueLabel = getLabel(LABEL_VALUE);
		String descriptionLabel = getLabel(LABEL_DESCRIPTION);
		p.setValue(TableEditor.HEADERS, new String[]{nameLabel, valueLabel, descriptionLabel});

		p = property(PROPERTY_SPRING_CONFIG_LOCATIONS);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, new ArrayList<>());
		p.setPropertyEditorClass(TableEditor.class);
		p.setValue(TableEditor.CLASSNAME, String.class.getName());
		p.setValue(TableEditor.OBJECT_PROPERTIES, new String[]{"value"});
		String patternLabel = getLabel(LABEL_PATTERN);
		p.setValue(TableEditor.HEADERS, new String[]{patternLabel});
	}

	protected String getLabel(String key) {
		return messageFunction.apply(key);
	}

	/**
	 * @return display name for TestElement
	 */
	protected String getDisplayName() {
		return messageFunction.apply("displayName");
	}

	protected String getDisplayName(String property) {
		String key = String.format("%s.displayName", property);
		return messageFunction.apply(key);
	}

	protected Function<String, String> getMessageFunction() {
		ResourceBundle bundle = getResourceBundle();
		return new MessageFunction(bundle);
	}

	@Nonnull
	protected ResourceBundle getResourceBundle() {
		Object value = super.getBeanDescriptor().getValue(RESOURCE_BUNDLE);
		return ResourceBundle.class.cast(value);
	}

	protected static class MessageFunction implements Function<String, String> {

		protected final ResourceBundle bundle;

		protected MessageFunction(ResourceBundle bundle) {
			this.bundle = bundle;
		}

		@Override
		public String apply(String key) {
			checkNotNull(key, "null String");
			String trimmed = bundle.containsKey(key) ? bundle.getString(key).trim() : "";
			return trimmed.isEmpty() ? key : trimmed;
		}
	}
}
