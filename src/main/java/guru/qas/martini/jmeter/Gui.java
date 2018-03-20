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

package guru.qas.martini.jmeter;

import java.awt.Font;

import javax.swing.JLabel;

import org.apache.jmeter.gui.JMeterGUIComponent;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;

import guru.qas.martini.MartiniException;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class Gui {

	protected static final String KEY_TITLE = "error.dialog.title";

	protected Gui() {
	}

	public static void reportError(TestElement element, MartiniException e) {
		String description = e.getMessage();
		showError(element, description, e);
	}

	protected static void showError(TestElement element, String description, Exception e) {
		String title = getTitle(element);
		JMeterUtils.reportErrorToUser(description, title, e);
	}

	public static void reportError(TestElement element, String key, Exception e) {
		Class<? extends TestElement> implementation = element.getClass();
		String description = I18n.getMessage(implementation, key);
		showError(element, description, e);
	}

	private static String getTitle(TestElement element) {
		Class<? extends TestElement> implementation = element.getClass();
		return I18n.getMessage(implementation, KEY_TITLE, element.getName());
	}

	public static JLabel getJLabel(Class<? extends JMeterGUIComponent> implementation, String key, int sizeAdjustment) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");

		String label = I18n.getMessage(implementation, key);
		JLabel jLabel = new JLabel(label);
		Font sourceFont = jLabel.getFont();
		Font font = sourceFont.deriveFont((float) sourceFont.getSize() + sizeAdjustment);
		jLabel.setFont(font);
		return jLabel;
	}
}
