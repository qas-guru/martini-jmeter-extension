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

import javax.annotation.Nonnull;
import javax.swing.JLabel;

import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.support.ResourceBundleMessageSource;

import static org.apache.jmeter.util.JMeterUtils.getLocale;

@SuppressWarnings("WeakerAccess")
public class Gui {

	protected static final Gui INSTANCE = new Gui();
	protected static final String KEY_DIALOG_TITLE = "error.dialog.title";
	protected final ResourceBundleMessageSource messageBundle;

	protected Gui() {
		messageBundle = new ResourceBundleMessageSource();
		messageBundle.setBasename(getClass().getName());
		messageBundle.setBundleClassLoader(getClass().getClassLoader());
	}

	public static void reportError(TestElement element, String description) {
		String title = getErrorDialogTitle(element);
		JMeterUtils.reportErrorToUser(description, title);
	}

	public static void reportError(TestElement element, Exception e) {
		String description = e.getMessage();
		reportError(element, description, e);
	}

	public static void reportError(TestElement element, String description, Exception e) {
		String title = getErrorDialogTitle(element);
		JMeterUtils.reportErrorToUser(description, title, e);
	}

	protected static String getErrorDialogTitle(TestElement element) {
		String name = element.getName();
		return INSTANCE.messageBundle.getMessage(KEY_DIALOG_TITLE, new Object[]{name}, KEY_DIALOG_TITLE, getLocale());
	}

	public static JLabel getJLabel(@Nonnull String text, int sizeAdjustment) {
		JLabel jLabel = new JLabel(text);
		Font sourceFont = jLabel.getFont();
		Font font = sourceFont.deriveFont((float) sourceFont.getSize() + sizeAdjustment);
		jLabel.setFont(font);
		return jLabel;
	}
}
