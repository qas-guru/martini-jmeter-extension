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

import java.io.Serializable;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.TestCompilerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import guru.qas.martini.jmeter.Gui;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniController extends AbstractTestElement implements Controller, Serializable, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = -3785811213682702141L;

	protected transient Logger logger;
	protected transient Controller delegate;

	public AbstractMartiniController() {
		super();
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public Object clone() {
		AbstractMartiniController clone = AbstractMartiniController.class.cast(super.clone());
		if (NoThreadClone.class.isInstance(delegate)) {
			clone.delegate = delegate;
		}
		else if (Controller.class.isInstance(delegate)) {
			clone.delegate = Controller.class.cast(delegate.clone());
		}
		return clone;
	}

	@Override
	public void testStarted() {
		try {
			initializeDelegate();
			TestStateListener listener = getAs(TestStateListener.class, delegate);
			if (null != listener) {
				listener.testStarted();
			}
		}
		catch (Exception e) {
			delegate = null;
			this.clearTestElementChildren();
			logger.error("{}: testStarted() failure", getName(), e);
			Gui.reportError(this, "An error occurred during startup; see logs for details.");
		}
	}

	protected <T> T getAs(Class<T> implementation, Object o) {
		return implementation.isInstance(o) ? implementation.cast(o) : null;
	}

	protected void stopTestNowOnException(Runnable runnable, String message) {
		try {
			runnable.run();
		}
		catch (Exception e) {
			this.clearTestElementChildren();
			logger.error(message, e);
			Gui.reportError(this, "An error occurred during startup; see logs for details.");
			JMeterContextService.getContext().getEngine().stopTest(true);
		}
	}

	protected void initializeDelegate() {
		try {
			delegate = createDelegate();
			checkNotNull(delegate, "method createDelegate() returned null");
			delegate.setName(getName());
			delegate.setComment(getComment());
		}
		catch (Exception e) {
			throw new RuntimeException("unable to initialize delegate", e);
		}
	}

	@Nonnull
	protected abstract Controller createDelegate() throws Exception;

	@Override
	public void addIterationListener(LoopIterationListener listener) {
		if (null != delegate) {
			delegate.addIterationListener(listener);
		}
	}

	@Override
	public void addTestElement(TestElement element) {
		if (null != delegate) {
			delegate.addTestElement(element);
		}
	}

	@Override
	public boolean addTestElementOnce(TestElement child) {
		TestCompilerHelper helper = getAs(TestCompilerHelper.class, delegate);
		boolean evaluation = false;
		if (null != helper) {
			evaluation = helper.addTestElementOnce(child);
		}
		else if (null != delegate) {
			delegate.addTestElement(child);
		}
		return evaluation;
	}

	@Override
	public void testStarted(String host) {
		try {
			initializeDelegate();
			TestStateListener listener = getAs(TestStateListener.class, delegate);
			if (null != listener) {
				listener.testStarted(host);
			}
		}
		catch (Exception e) {
			delegate = null;
			this.clearTestElementChildren();
			logger.error("{}: testStarted(String) failure", getName(), e);
			Gui.reportError(this, "An error occurred during startup; see logs for details.");
		}
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		stopTestNowOnException(() -> {
			LoopIterationListener listener = getAs(LoopIterationListener.class, delegate);
			if (null != listener) {
				listener.iterationStart(event);
			}
		}, String.format("%s: %s.iterationStart(LoopIterationEvent) failure", getName(), delegate));
	}

	@Override
	public void initialize() {
		if (null != delegate) {
			stopTestNowOnException(() -> delegate.initialize(),
				String.format("%s: %s.initialize() failure", getName(), delegate));
		}
	}

	@Override
	public Sampler next() {
		return null == delegate ? null : stopTestNowOnException(() -> delegate.next(),
			String.format("%s: %s.next() failure", getName(), delegate));
	}

	protected <T> T stopTestNowOnException(Callable<T> callable, String message) {
		T evaluation = null;
		try {
			evaluation = callable.call();
		}
		catch (Exception e) {
			this.clearTestElementChildren();
			logger.error(message, e);
			Gui.reportError(this, "An error occurred during startup; see logs for details.");
			JMeterContextService.getContext().getEngine().stopTest(true);
		}
		return evaluation;
	}

	@Override
	public boolean isDone() {
		return null == delegate ? true : stopTestNowOnException(() -> delegate.isDone(),
			String.format("%s: %s.isDone() failure", getName(), delegate));
	}

	@Override
	public void triggerEndOfLoop() {
		if (null != delegate) {
			stopTestNowOnException(() -> delegate.triggerEndOfLoop(),
				String.format("%s: %s.triggerEndOfLoop() failure", getName(), delegate));
		}
	}

	@Override
	public void removeIterationListener(LoopIterationListener listener) {
		if (null != delegate) {
			stopTestNowOnException(() -> delegate.removeIterationListener(listener),
				String.format("%s: %s.removeIterationListener(LoopIterationListener) failure", getName(), delegate));
		}
	}

	@Override
	public void testEnded() {
		try {
			TestStateListener listener = getAs(TestStateListener.class, delegate);
			if (null != listener) {
				listener.testEnded();
			}
		}
		catch (Exception e) {
			logger.warn("{}: unable to execute testEnded on delegate {}", getName(), delegate, e);
		}
		finally {
			destroyDelegate();
		}
	}

	@Override
	public void testEnded(String host) {
		try {
			TestStateListener listener = getAs(TestStateListener.class, delegate);
			if (null != listener) {
				listener.testEnded(host);
			}
		}
		catch (Exception e) {
			logger.warn("{}: unable to execute testEnded(String) on delegate {}", getName(), delegate, e);
		}
		finally {
			destroyDelegate();
		}
	}

	protected void destroyDelegate() {
		DisposableBean disposable = getAs(DisposableBean.class, delegate);
		if (null != disposable) {
			try {
				disposable.destroy();
			}
			catch (Exception e) {
				logger.warn("{}: unable to execute destroy() on delegate {}", getName(), delegate, e);
			}
		}
		delegate = null;
	}
}
