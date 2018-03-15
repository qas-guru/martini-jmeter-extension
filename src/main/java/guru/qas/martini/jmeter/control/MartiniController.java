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
import java.util.Deque;
import java.util.LinkedHashSet;

import org.apache.jmeter.control.Controller;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.TestCompilerHelper;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends AbstractTestElement implements Controller, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = 2700570246170278883L;
	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	protected transient LinkedHashSet<TestElement> subElements;
	protected transient Deque<TestElement> elementDeque;

	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	@Override
	public Object clone() {
		MartiniController clone = MartiniController.class.cast(super.clone());
		System.out.println("CLONE: " + System.identityHashCode(clone));
		return clone;
	}

	@Override
	public void setRunningVersion(boolean runningVersion) {
		System.out.println("SET RUNNING VERSION: " + runningVersion + " " + System.identityHashCode(this));
		super.setRunningVersion(runningVersion);
		if (runningVersion) {
			subElements = new LinkedHashSet<>();
			elementDeque = new ArrayDeque<>();
		}
	}

	@Override
	public void testStarted() {
		System.out.println("TEST STARTED: " + System.identityHashCode(this));
	}

	@Override
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void addTestElement(TestElement element) {
		System.out.println("ADD TEST ELEMENT: " + System.identityHashCode(this));
		System.out.println("IS RUNNING VERSION: " + this.isRunningVersion());
		addTestElementOnce(element);
	}

	@Override
	public boolean addTestElementOnce(TestElement element) {
		System.out.println("ADD TEST ELEMENT ONCE: " + System.identityHashCode(this));
		boolean evaluation = false;
		if (this.isRunningVersion() && (Sampler.class.isInstance(element) || Controller.class.isInstance(element))) {
			evaluation = subElements.add(element);
		}
		return evaluation;
	}

	@Override
	public void addIterationListener(LoopIterationListener listener) {
		System.out.println("ADD ITERATION LISTENER: " + System.identityHashCode(this));
	}

	@Override
	public void removeIterationListener(LoopIterationListener iterationListener) {
		System.out.println("breakpoint");
	}

	@Override
	public void initialize() {
		System.out.println("INITIALIZE: " + System.identityHashCode(this));
	}

	@Override
	public void iterationStart(LoopIterationEvent iterEvent) {
		System.out.println("LOOP ITERATION START: " + System.identityHashCode(this));
		elementDeque = new ArrayDeque<>(subElements); // TODO: advance que
	}

	@Override
	public boolean isDone() {
		System.out.println("IS DONE: " + System.identityHashCode(this));
		TestElement current = advanceElementDeque();
		if (null == current) {
			triggerEndOfLoop();
		}
		return false;
	}

	protected TestElement advanceElementDeque() {
		TestElement peek = elementDeque.peek();
		while (Controller.class.isInstance(peek) && Controller.class.cast(peek).isDone()) {
			elementDeque.pop();
			peek = elementDeque.peek();
		}
		return peek;
	}

	@Override
	public Sampler next() {
		System.out.println("NEXT: " + System.identityHashCode(this));

		Sampler sampler = null;
		TestElement peek = advanceElementDeque();
		if (Controller.class.isInstance(peek)) {
			Controller subController = Controller.class.cast(peek);
			sampler = subController.next();
		}
		else if (null != peek) {
			TestElement pop = elementDeque.pop();
			sampler = Sampler.class.isInstance(pop) ? Sampler.class.cast(pop) : null;
		}
		return sampler;
	}

	@Override
	public void triggerEndOfLoop() {
		System.out.println("TRIGGER END OF LOOP: " + System.identityHashCode(this));
		elementDeque.clear();
		elementDeque.addAll(subElements);
		advanceElementDeque();
	}

	@Override
	public void testEnded() {
		System.out.println("TEST ENDED: " + System.identityHashCode(this));
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}
