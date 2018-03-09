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

	protected static final Gui INSTANCE = new Gui();
	protected static final String KEY_TITLE = "error.dialog.title";

	private final Il8n il8n;

	protected Gui() {
		this.il8n = Il8n.getInstance();
	}

	public void reportError(Class<? extends TestElement> implementation, MartiniException e) {
		String title = getTitle(implementation);
		String message = e.getMessage();
		JMeterUtils.reportErrorToUser(message, title, e);
	}

	public void reportError(Class<? extends TestElement> implementation, String key, Exception e) {
		String title = getTitle(implementation);
		String message = il8n.getMessage(implementation, key);
		JMeterUtils.reportErrorToUser(message, title, e);
	}

	private String getTitle(Class<? extends TestElement> implementation) {
		return il8n.getMessage(implementation, KEY_TITLE);
	}

	public JLabel getJLabel(Class<? extends JMeterGUIComponent> implementation, String key, int sizeAdjustment) {
		checkNotNull(implementation, "null Class");
		checkNotNull(key, "null String");

		String label = il8n.getMessage(implementation, key);
		JLabel jLabel = new JLabel(label);
		Font spelLabelFont = jLabel.getFont();
		Font font = spelLabelFont.deriveFont((float) spelLabelFont.getSize() + sizeAdjustment);
		jLabel.setFont(font);
		return jLabel;
	}

	public static Gui getInstance() {
		return INSTANCE;
	}


}
