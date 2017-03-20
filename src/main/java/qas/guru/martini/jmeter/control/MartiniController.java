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

package qas.guru.martini.jmeter.control;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.util.JMeterStopTestException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Monitor;

import gherkin.ast.Step;
import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.step.StepImplementation;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements Serializable, TestStateListener {

	protected volatile transient Monitor monitor;
	protected volatile transient AtomicReference<ImmutableList<Martini>> martinisRef;
	protected volatile transient AtomicReference<Iterator<Martini>> iteratorRef;

	public MartiniController() {
		super();
		monitor = new Monitor();
		martinisRef = new AtomicReference<>();
		iteratorRef = new AtomicReference<>();
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniController clone = MartiniController.class.cast(o);
		clone.monitor = this.monitor;
		clone.martinisRef = this.martinisRef;
		clone.iteratorRef = this.iteratorRef;
		return clone;
	}

	@Override
	public void initialize() {
		super.initialize();
		monitor.enter();
		try {
			if (null == getMartinis()) {
				JMeterVariables variables = getJMeterVariables();
				initialize(variables);
			}
		}
		finally {
			monitor.leave();
		}
	}

	private ImmutableList<Martini> getMartinis() {
		monitor.enter();
		try {
			return martinisRef.get();
		}
		finally {
			monitor.leave();
		}
	}

	private JMeterVariables getJMeterVariables() {
		JMeterContext threadContext = getThreadContext();
		return threadContext.getVariables();
	}

	protected void initialize(JMeterVariables variables) {
		Object o = variables.getObject("applicationContext"); // TODO: a constant
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);
		Mixologist mixologist = context.getBean(Mixologist.class);
		initialize(mixologist);
	}

	protected void initialize(Mixologist mixologist) {
		Iterable<Martini> martinis = mixologist.getMartinis(); // TODO: scenario filtering
		if (Iterables.isEmpty(martinis)) {
			String message = String.format("%s:%s has no scenarios to run", getClass().getName(), getName());
			throw new JMeterStopTestException(message);
		}

		Martini template = Iterables.getFirst(martinis, null);
		List<Martini> temps = Lists.newArrayList();
		for (int i = 0; i < 3; i++) {
			temps.add(new MartiniWrapper("Martini " + i, template));
		}
		//martinisRef.set(ImmutableList.copyOf(martinis));
		martinisRef.set(ImmutableList.copyOf(temps));
		resetIterator();
	}

	static final class MartiniWrapper implements Martini {

		private final String label;
		private final Martini martini;

		MartiniWrapper(String label, Martini martini) {
			this.label = label;
			this.martini = martini;
		}

		@Override
		public Recipe getRecipe() {
			return martini.getRecipe();
		}

		@Override
		public Map<Step, StepImplementation> getStepIndex() {
			return martini.getStepIndex();
		}

		@Override
		public String toString() {
			return label;
		}
	}

	protected Martini martini;

	@Override
	protected void fireIterationStart() {
		super.fireIterationStart();
		monitor.enter();
		try {
			Iterator<Martini> iterator = this.iteratorRef.get();
			martini = null != iterator && iterator.hasNext() ? iterator.next() : null;
		}
		finally {
			monitor.leave();
		}
	}

	@Override
	protected TestElement getCurrentElement() throws NextIsNullException {
		TestElement element = super.getCurrentElement();

		if (null != element && null != martini) {
			JMeterVariables variables = getJMeterVariables();
			variables.putObject("martini", martini);
		}
		else if (null != element) {
			element = null;
		}
		else {
			monitor.enter();
			try {
				Iterator<Martini> iterator = iteratorRef.get();
				if (iterator.hasNext()) {
					martini = iterator.next();
					resetCurrent();
					element = this.getCurrentElement();
				}
			}
			finally {
				monitor.leave();
			}
		}
		return element;
	}

	@Override
	public void testStarted() {
		resetIterator();
	}

	@Override
	public void triggerEndOfLoop() {
		super.triggerEndOfLoop();
		resetIterator();
	}

	private void resetIterator() {
		monitor.enter();
		try {
			Iterator<Martini> iterator = iteratorRef.get();
			if (null == iterator) {
				ImmutableList<Martini> martinis = getMartinis();
				if (null != martinis) {
					iterator = martinis.iterator();
					iteratorRef.set(iterator);
				}
			}
		}
		finally {
			monitor.leave();
		}
	}

	@Override
	public void testStarted(String host) {
	}

	@Override
	public void testEnded() {
		iteratorRef.set(null);
	}

	@Override
	public void testEnded(String host) {
	}
}