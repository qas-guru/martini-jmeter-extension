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

package guru.qas.martini.jmeter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.result.MartiniResult;

@SuppressWarnings({"unused"})
public class MartiniPreProcessor extends AbstractTestElement
	implements PreProcessor, NoThreadClone, LoopIterationListener, TestStateListener {

	private static final long serialVersionUID = -1836250604581459661L;

	public static final String VARIABLE = "martini";

	public static final String ARGUMENT_SPEL_FILTER = "spel.filter";
	private static final String DEFAULT_SPEL_FILTER = "!isWIP()";

	public static final String ARGUMENT_CYCLIC_ITERATOR = "cyclic.iteration";
	private static final String DEFAULT_CYCLIC_ITERATOR = "true";

	public static final String ARGUMENT_SHUFFLED_ITERATION = "shuffled.iteration";
	private static final String DEFAULT_SHUFFLED_ITERATION = "false";

	private static final Arguments ARGUMENTS = new Arguments();

	static {
		ARGUMENTS.addArgument(ARGUMENT_SPEL_FILTER, DEFAULT_SPEL_FILTER, null,
			"Spring Expression Language filter");
		ARGUMENTS.addArgument(ARGUMENT_CYCLIC_ITERATOR, DEFAULT_CYCLIC_ITERATOR, null,
			"true to cycle through scenarios indefinitely");
		ARGUMENTS.addArgument(ARGUMENT_SHUFFLED_ITERATION, DEFAULT_SHUFFLED_ITERATION, null,
			"true to randomly shuffle scenarios");
	}

	public static Arguments getDefaultArguments() {
		return ARGUMENTS;
	}

	private transient Monitor monitor;
	private transient AtomicReference<Iterator<Martini>> ref;

	public MartiniPreProcessor() {
		super();
	}

	@Override
	public void testStarted() {
		monitor = new Monitor();
		ref = new AtomicReference<>();
	}

	@Override
	public void testStarted(String s) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent loopIterationEvent) {
		JMeterContext context = JMeterContextService.getContext();

		MartiniResult result;
		Martini next = getNext();

		JMeterVariables variables = context.getVariables();
		if (null == next) {
			variables.remove(VARIABLE);
		}
		else {
			variables.putObject(VARIABLE, next);
		}
	}

	static Martini getMartini() {
		JMeterContext context = JMeterContextService.getContext();
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(VARIABLE);
		return Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
	}

	private Martini getNext() {
		monitor.enter();
		try {
			Iterator<Martini> i = getIterator();
			return i.hasNext() ? i.next() : null;
		}
		finally {
			monitor.leave();
		}
	}

	private Iterator<Martini> getIterator() {
		Iterator<Martini> i = ref.get();
		if (null == i) {
			Mixologist mixologist = getMixologist();
			i = getIterator(mixologist);
			ref.set(i);
		}
		return i;
	}

	private static Mixologist getMixologist() {
		ApplicationContext springContext = SpringPreProcessor.getApplicationContext();
		return springContext.getBean(Mixologist.class);
	}

	private Iterator<Martini> getIterator(Mixologist mixologist) {
		String spelFilter = super.getPropertyAsString(ARGUMENT_SPEL_FILTER).trim();
		Collection<Martini> martiniCollection =
			spelFilter.isEmpty() ? mixologist.getMartinis() : mixologist.getMartinis(spelFilter);
		List<Martini> martinis = new ArrayList<>(martiniCollection);

		boolean shuffled = super.getPropertyAsBoolean(ARGUMENT_SHUFFLED_ITERATION);
		if (shuffled) {
			SecureRandom random = new SecureRandom();
			Collections.shuffle(martinis, random);
		}

		boolean cyclic = super.getPropertyAsBoolean(ARGUMENT_CYCLIC_ITERATOR);
		return cyclic ? Iterators.cycle(martinis) : Iterators.consumingIterator(martinis.iterator());
	}

	@Override
	public void process() {
	}

	@Override
	public void testEnded() {
		monitor = null;
		ref = null;
	}

	@Override
	public void testEnded(String s) {
		testEnded();
	}
}