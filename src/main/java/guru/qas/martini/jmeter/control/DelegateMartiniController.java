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

import java.util.concurrent.atomic.AtomicReference;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import guru.qas.martini.Martini;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.JMeterContextUtil;
import guru.qas.martini.jmeter.Parameterized;
import guru.qas.martini.jmeter.processor.MartiniSpringPreProcessor;

import static guru.qas.martini.jmeter.control.MartiniController.PROPERTY_SPEL_FILTER;

@SuppressWarnings("WeakerAccess")
public class DelegateMartiniController extends GenericController implements Cloneable, TestStateListener, LoopIterationListener {

	private transient volatile MessageSource messageSource;
	private transient volatile AtomicReference<LoopIterationListener> listenerRef;
	private transient volatile Logger logger;

	public DelegateMartiniController() {
		super();
		this.messageSource = MessageSources.getMessageSource(getClass());
		this.listenerRef = new AtomicReference<>();
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public Object clone() {
		DelegateMartiniController clone = DelegateMartiniController.class.cast(super.clone());
		clone.messageSource = messageSource;
		clone.listenerRef = listenerRef;
		return clone;
	}

	@Override
	public void testStarted() {
		testStarted(null);
	}

	@Override
	public void testStarted(String host) {
		LoopIterationListener listener = getListener();
		listenerRef.set(listener);

		if (TestStateListener.class.isInstance(listener)) {
			try {
				TestStateListener asTestStateListener = TestStateListener.class.cast(listener);
				asTestStateListener.testStarted(host);
			}
			catch (Exception e) {
				logger.warn("encountered listener problem on start: {}", listener, e);
			}
		}
	}

	protected LoopIterationListener getListener() {
		Parameterized configuration = JMeterContextUtil.getProperty(this, Parameterized.class)
			.orElseThrow(() -> new IllegalStateException("unable to find Parameterized property"));
		String parameter = configuration.getParameter(PROPERTY_SPEL_FILTER).orElse("").trim();
		String spelFilter = parameter.isEmpty() ? null : parameter.trim();
		ApplicationContext springContext = MartiniSpringPreProcessor.getApplicationContext();
		return new MartiniLoopIterationListener(springContext, spelFilter);
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		LoopIterationListener listener = listenerRef.get();
		if (null != listener) {
			listener.iterationStart(event);
		}
		advanceIterator();
	}

	protected void advanceIterator() {
		Martini martini = getNextMartini();
		JMeterContextUtil.setVariable(martini, Martini.class);
	}

	protected Martini getNextMartini() {
		LockingIterator<Martini> iterator = getIterator();
		try {
			iterator.lockInterruptibly();
			return iterator.hasNext() ? iterator.next() : null;
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		finally {
			iterator.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	protected LockingIterator<Martini> getIterator() {
		LockingIterator i = JMeterContextUtil.getVariable(LockingIterator.class)
			.orElseThrow(() -> new IllegalStateException("LockingIterator<Martini> not found"));
		return (LockingIterator<Martini>) i;
	}

	@Override
	public Sampler next() {
		Sampler next = super.next();
		return null == next ? super.next() : next;
	}

	@Override
	protected void fireIterationStart() {
		advanceIterator();
		super.fireIterationStart();
	}

	@Override
	protected Sampler nextIsASampler(Sampler element) throws NextIsNullException {
		Sampler sampler = super.nextIsASampler(element);
		Martini martini = null == sampler ? null : JMeterContextUtil.getVariable(Martini.class).orElse(null);

		if (null == martini) {
			sampler = null;
		}
		else {
			JMeterContextUtil.setSamplerData(sampler, martini, Martini.class);
		}
		return sampler;
	}

	@Override
	public void testEnded() {
		testEnded(null);
	}

	@Override
	public void testEnded(String host) {
		releaseListener();
		listenerRef = null;
		messageSource = null;
		logger = null;
	}

	protected void releaseListener() {
		LoopIterationListener listener = listenerRef.get();

		if (TestStateListener.class.isInstance(listener)) {
			try {
				TestStateListener asStateListener = TestStateListener.class.cast(listener);
				asStateListener.testEnded();
			}
			catch (Exception e) {
				logger.warn("unable to cleanly release listener", e);
			}
		}
	}
}