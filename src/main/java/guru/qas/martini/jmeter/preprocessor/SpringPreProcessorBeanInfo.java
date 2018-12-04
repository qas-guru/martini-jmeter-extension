package guru.qas.martini.jmeter.preprocessor;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;

import org.apache.jmeter.config.Argument;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.gui.TableEditor;

import static guru.qas.martini.jmeter.preprocessor.SpringPreProcessor.*;

@SuppressWarnings({"unused", "WeakerAccess"})
public class SpringPreProcessorBeanInfo extends BeanInfoSupport {

	public SpringPreProcessorBeanInfo() {
		super(SpringPreProcessor.class);

		createPropertyGroup("Options", new String[]{PROPERTY_ENVIRONMENT_VARIABLES, PROPERTY_SPRING_CONFIG_LOCATIONS});

		PropertyDescriptor p = property(PROPERTY_ENVIRONMENT_VARIABLES);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, new ArrayList<Argument>());
		p.setPropertyEditorClass(TableEditor.class);
		p.setValue(TableEditor.CLASSNAME, Argument.class.getName());
		p.setValue(TableEditor.OBJECT_PROPERTIES, new String[]{"name", "value", "description"});  // TODO: il8n
		p.setValue(TableEditor.HEADERS, new String[]{"name", "value", "description"}); // TODO: il8n

		p = property(PROPERTY_SPRING_CONFIG_LOCATIONS);
		p.setValue(NOT_UNDEFINED, Boolean.TRUE);
		p.setValue(DEFAULT, new ArrayList<>());
		p.setPropertyEditorClass(TableEditor.class);
		p.setValue(TableEditor.CLASSNAME, String.class.getName());
		p.setValue(TableEditor.OBJECT_PROPERTIES, new String[]{"value"}); // TODO: il8n
		p.setValue(TableEditor.HEADERS, new String[]{"pattern"}); // TODO: il8n
	}
}