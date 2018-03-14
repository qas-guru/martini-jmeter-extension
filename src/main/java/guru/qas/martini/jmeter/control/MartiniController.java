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

package guru.qas.martini.jmeter.control;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.Il8n;

import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 2700570246170278883L;
	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	protected transient Monitor monitor;
	protected transient Set<Integer> iterations;
	protected transient Deque<Martini> martinis;

	public MartiniController() {
	}

	@Override
	public Object clone() {
		MartiniController clone = MartiniController.class.cast(super.clone());
		clone.iterations = iterations;
		clone.monitor = monitor;
		clone.martinis = martinis;
		return clone;
	}

	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	@Override
	public void testStarted() {
		this.iterations = new HashSet<>();
		this.monitor = new Monitor();
		this.martinis = new ArrayDeque<>();
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		super.reInitialize();
		int iteration = event.getIteration();

		monitor.enter();
		try {
			if (iterations.add(iteration)) {
				System.out.println(String.format("\nThread %s adding Martinis for loop %s", Thread.currentThread(), iteration));

				JMeterVariables variables = super.getThreadContext().getVariables();
				Object o = variables.getObject(KEY_SPRING_CONTEXT);
				if (!ApplicationContext.class.isInstance(o)) {
					Il8n il8n = Il8n.getInstance();
					String message = il8n.getInterpolatedMessage(getClass(), "warning.spring.context.not.set", getName());
					MartiniException exception = new MartiniException(message);
					Gui.getInstance().reportError(getClass(), exception);
					throw exception;
				}

				ApplicationContext springContext = ApplicationContext.class.cast(o);
				Mixologist mixologist = springContext.getBean(Mixologist.class);
				String spelFilter = getSpelFilter();
				Collection<Martini> martiniCollection = spelFilter.isEmpty() ?
					mixologist.getMartinis() : mixologist.getMartinis(spelFilter);
				martinis.addAll(martiniCollection);
			}

			TestElement source = event.getSource();
			JMeterContext threadContext = source.getThreadContext();
			JMeterVariables variables = threadContext.getVariables();

			if (martinis.isEmpty()) {
				variables.remove("martini");
				super.setDone(true);
			}
			else {
				Martini martini = martinis.pop();
				variables.putObject("martini", martini);
			}

		}
		finally {
			monitor.leave();
		}
	}

	@Override
	public Sampler next() {
		Sampler next = super.next();
		if (null != next) {
			JMeterContext threadContext = next.getThreadContext();
			JMeterVariables variables = threadContext.getVariables();
			Object o = variables.getObject("martini");
			if (Martini.class.isInstance(o)) {
				Martini martini = Martini.class.cast(o);
				Map<String, Object> samplerContext = threadContext.getSamplerContext();
				samplerContext.put("martini", martini);
			}
			else {
				next = null;
				setDone(true);
			}
		}
		return next;
	}

	//
//	@Override
//	public Sampler next() {
//		Sampler next;
//
//		monitor.enter();
//		try {
//			next = super.next(); // TODO: isFirst();
//			if (null == next && null != martinis.peek()) {
//				super.reInitialize();
//				next = super.next();
//				if (next != null) {
//					JMeterContext threadContext = next.getThreadContext();
//					JMeterVariables variables = threadContext.getVariables();
//					variables.remove("martini");
//				}
//			}
//
//			Martini martini = null;
//			if (null != next) {
//				JMeterContext threadContext = next.getThreadContext();
//				JMeterVariables variables = threadContext.getVariables();
//				Object o = variables.getObject("martini");
//				martini = Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
//				if (null == martini && null != martinis.peek()) {
//					martini = martinis.pop();
//					variables.putObject("martini", martini);
//				} else if (null == martini && null == martinis.peek()) {
//					super.setDone(true);
//				}
//			}
//
//			next = null == martini ? null : next;
//			System.out.println(String.format("\nThread %s returning Sampler %s", Thread.currentThread(), next));
//			return next;
//		}
//		finally {
//			monitor.leave();
//		}
//	}
//
//	@Override
//	public void triggerEndOfLoop() {
//		System.out.println("END OF LOOP");
//		super.triggerEndOfLoop();
//	}

	@Override
	public void testEnded() {
		this.iterations = null;
		this.monitor = null;
		this.martinis = null;
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}
