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

package guru.qas.martini.jmeter.processor.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.jmeter.gui.util.VerticalPanel;
import org.apache.jorphan.gui.JLabeledField;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings({"WeakerAccess", "unused"})
@Deprecated
public class RadiosPanel<E extends Enum> extends VerticalPanel implements JLabeledField, ActionListener {

	private static final long serialVersionUID = 5598924127436725853L;

	private final Class<E> implementation;
	private final JLabel label;
	private final ButtonGroup buttonGroup;
	private final List<ChangeListener> changeListeners;

	public RadiosPanel(Class<E> implementation) {
		this(implementation, new JLabel());
	}

	public RadiosPanel(Class<E> implementation, JLabel label) {
		super();
		this.implementation = checkNotNull(implementation, "null Class");
		this.label = checkNotNull(label, "null JLabel");
		buttonGroup = new ButtonGroup();
		changeListeners = new ArrayList<>(3);
		super.add(this.label);
	}

	public void setLabel(String text) {
		label.setText(text);
	}

	public void addButton(E command, String text, boolean selected) {
		JRadioButton button = new JRadioButton(text);
		int ordinal = command.ordinal();
		button.setActionCommand(String.valueOf(ordinal));
		button.addActionListener(this);
		buttonGroup.add(button);
		this.add(button);
		button.setSelected(selected);
	}

	public void setSelected(E command) {
		checkNotNull(command, "null Enum");
		int ordinal = command.ordinal();
		setText(String.valueOf(ordinal));
	}

	@SuppressWarnings({"unchecked", "ConstantConditions", "unused"})
	public Optional<E> getSelected() {
		String text = getText();

		E match = null;
		if (null != text) {
			int ordinal = Integer.valueOf(text);
			Set<E> enums = EnumSet.allOf(implementation);
			return enums.stream()
				.filter(e -> ordinal == e.ordinal())
				.findFirst();
		}
		return Optional.ofNullable(match);
	}

	@Override
	public void setText(String command) {
		Enumeration<AbstractButton> buttons = buttonGroup.getElements();
		Iterators.forEnumeration(buttons).forEachRemaining(b -> setSelected(b, command));
	}

	protected void setSelected(AbstractButton button, String command) {
		ButtonModel model = button.getModel();
		String actionCommand = model.getActionCommand();
		boolean state = actionCommand.equals(command);
		buttonGroup.setSelected(model, state);
	}

	@Override
	public String getText() {
		ButtonModel selection = buttonGroup.getSelection();
		return selection.getActionCommand();
	}

	@Override
	public void addChangeListener(ChangeListener l) {
		changeListeners.add(l);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		ChangeEvent event = new ChangeEvent(this);
		changeListeners.forEach(l -> l.stateChanged(event));
	}

	@Override
	public List<JComponent> getComponentList() {
		ArrayList<JComponent> components = Lists.newArrayList(label);
		Enumeration<AbstractButton> buttons = buttonGroup.getElements();
		Iterators.addAll(components, Iterators.forEnumeration(buttons));
		return components;
	}
}