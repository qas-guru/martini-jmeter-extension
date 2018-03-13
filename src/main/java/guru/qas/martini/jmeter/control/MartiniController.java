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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.ApplicationContext;

import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.jmeter.Il8n;

import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements TestStateListener {

	private static final long serialVersionUID = 2700570246170278883L;
	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";

	protected transient Monitor monitor;
	protected transient AtomicReference<Deque<Martini>> ref;

	public MartiniController() {
//		monitor = new Monitor();
//		ref = new AtomicReference<>();
	}

	@Override
	public Object clone() {
		MartiniController clone = MartiniController.class.cast(super.clone());
		clone.monitor = monitor;
		clone.ref = ref;
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
		this.monitor = new Monitor();
		this.ref = new AtomicReference<>();
	}

	@Override
	public void testStarted(String host) {
		testStarted();

	}

	public void initialize() {
		monitor.enter();
		try {
			System.out.println("breakpoint");
//			if (null == martinis) {
//				JMeterVariables variables = super.getThreadContext().getVariables();
//				Object o = variables.getObject(KEY_SPRING_CONTEXT);
//				if (!ApplicationContext.class.isInstance(o)) {
//					Il8n il8n = Il8n.getInstance();
//					String message = il8n.getInterpolatedMessage(getClass(), "warning.spring.context.not.set", getName());
//					throw new MartiniException(message);
//				}
//
//				ApplicationContext springContext = ApplicationContext.class.cast(o);
//				Mixologist mixologist = springContext.getBean(Mixologist.class);
//				String spelFilter = getSpelFilter();
//				Collection<Martini> martiniCollection = spelFilter.isEmpty() ?
//					mixologist.getMartinis() : mixologist.getMartinis(spelFilter);
//				martinis = new ArrayDeque<>(martiniCollection);
//			}
		}
		finally {
			monitor.leave();
		}
	}

	@Override
	protected void initializeSubControllers() {
		super.initializeSubControllers();
	}

	@Override
	protected void reInitialize() {
		super.reInitialize();
	}

	@Override
	public Sampler next() {
		return super.next();
	}

	@Override
	public boolean isDone() {
		return super.isDone();
	}

	@Override
	public void triggerEndOfLoop() {
		super.triggerEndOfLoop();
	}

	@Override
	public void testEnded() {
		this.monitor = null;
		this.ref = null;
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}

//	@Override
//	public void iterationStart(LoopIterationEvent event) {
//
//		monitor.enter();
//		try {
//			initialize();
//			TestElement source = event.getSource();
//			JMeterProperty property = source.getProperty(key);
//			Object o = property.getObjectValue();
//			if (GenericController.class.isInstance(o)) {
//				GenericController controller = GenericController.class.cast(o);
//				controller.setRunningVersion(true);
//			}
//			listeners.forEach(l -> l.iterationStart(event));
//		}
//		finally {
//			monitor.leave();
//		}
//	}
}
