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
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.TestCompilerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.jmeter.Gui;

import static com.google.common.base.Preconditions.checkState;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniController extends AbstractTestElement
	implements Controller, Serializable, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = -3785811213682702141L;

	protected transient Logger logger;
	protected transient Monitor monitor;
	protected transient AtomicReference<Controller> delegateRef;

	public AbstractMartiniController() {
		super();
		init();
	}

	protected void init() {
		logger = LoggerFactory.getLogger(getClass());
		monitor = new Monitor();
		delegateRef = new AtomicReference<>();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	@Override
	public Object clone() {
		AbstractMartiniController clone = AbstractMartiniController.class.cast(super.clone());

		Controller delegate = delegateRef.get();
		NoThreadClone noThreadClone = null == delegate ? null : getDelegateAs(NoThreadClone.class);
		if (null != noThreadClone) {
			clone.monitor = monitor;
			clone.delegateRef = delegateRef;
		}
		else if (null != delegate) {
			try {
				Controller delegateClone = Controller.class.cast(delegate.clone());
				clone.delegateRef.set(delegateClone);
			}
			catch (Exception e) {
				logger.error("{}: clone() failure", getName(), e);
				abortStartup();
			}
		}
		return clone;
	}

	protected <T> T getDelegateAs(Class<T> implementation) {
		Controller delegate = delegateRef.get();
		return implementation.isInstance(delegate) ? implementation.cast(delegate) : null;
	}

	protected void abortStartup() {
		destroyDelegate();
		Gui.reportError(this, "An error occurred during startup; see logs for details.");
	}

	@Override
	public void testStarted() {
		try {
			setDelegate();
			TestStateListener listener = getDelegateAs(TestStateListener.class);
			if (null != listener) {
				listener.testStarted();
			}
		}
		catch (Exception e) {
			logger.error("{}: testStarted() failure", getName(), e);
			abortStartup();
		}
	}

	@Override
	public void testStarted(String host) {
		try {
			setDelegate();
			TestStateListener listener = getDelegateAs(TestStateListener.class);
			if (null != listener) {
				listener.testStarted(host);
			}
		}
		catch (Exception e) {
			logger.error("{}: testStarted(String) failure", getName(), e);
			abortStartup();
		}
	}

	protected void setDelegate() throws Exception {
		Controller delegate = createDelegate();
		delegate.setName(getName());
		delegate.setComment(getComment());
		checkState(delegateRef.compareAndSet(null, delegate), "delegate already initialized");
	}

	@Nonnull
	protected abstract Controller createDelegate() throws Exception;

	@Override
	public void addIterationListener(LoopIterationListener listener) {
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			try {
				delegate.addIterationListener(listener);
			}
			catch (Exception e) {
				logger.error("{}: addIterationListener(LoopIterationListener) failure", getName(), e);
				abortStartup();
			}
		}
	}

	@Override
	public void addTestElement(TestElement element) {
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			try {
				delegate.addTestElement(element);
				if (LoopIterationListener.class.isInstance(element)) {
					LoopIterationListener listener = LoopIterationListener.class.cast(element);
					this.addIterationListener(listener);
				}
			}
			catch (Exception e) {
				logger.error("{}: addTestElement(TestElement) failure", getName(), e);
				abortStartup();
			}
		}
	}

	@Override
	public boolean addTestElementOnce(TestElement child) {
		TestCompilerHelper testCompilerHelper = getDelegateAs(TestCompilerHelper.class);

		boolean evaluation = true;
		if (null != testCompilerHelper) {
			try {
				evaluation = testCompilerHelper.addTestElementOnce(child);
			}
			catch (Exception e) {
				logger.error("{}: addTestElementOnce(TestElement) failure", getName(), e);
				abortStartup();
			}
		}
		else {
			addTestElement(child);
		}
		return evaluation;
	}

	@Override
	public void initialize() {
		Controller delegate = delegateRef.get();
		if (null == delegate) {
			stopTestNow();
		}
		else {
			try {
				delegate.initialize();
			}
			catch (Exception e) {
				logger.error("{}: initialize() failure", getName(), e);
				stopTestNow();
			}
		}
	}

	protected void stopTestNow() {
		this.destroyDelegate();
		Gui.reportError(this, "An error occurred during test run; see logs for details.");
		JMeterContext context = JMeterContextService.getContext();
		StandardJMeterEngine engine = context.getEngine();
		engine.stopTest(true);
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		LoopIterationListener listener = getDelegateAs(LoopIterationListener.class);
		if (null != listener) {
			try {
				listener.iterationStart(event);
			}
			catch (Exception e) {
				logger.error("{}: iterationStart(LoopIterationEvent) failure", getName(), e);
				stopTestNow();
			}
		}
	}

	@Override
	public Sampler next() {
		Controller delegate = delegateRef.get();
		Sampler next = null;
		if (null != delegate) {
			try {
				next = delegate.next();
			}
			catch (Exception e) {
				logger.error("{}: next() failure", getName(), e);
				stopTestNow();
			}
		}
		return next;
	}

	@Override
	public boolean isDone() {
		Controller delegate = delegateRef.get();
		boolean evaluation = true;
		if (null != delegate) {
			try {
				evaluation = delegate.isDone();
			}
			catch (Exception e) {
				logger.error("{}: isDone() failure", getName(), e);
				stopTestNow();
			}
		}
		return evaluation;
	}

	@Override
	public void triggerEndOfLoop() {
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			try {
				delegate.triggerEndOfLoop();
			}
			catch (Exception e) {
				logger.error("{}, triggerEndOfLoop() failure", getName(), e);
				stopTestNow();
			}
		}
	}

	@Override
	public void removeIterationListener(LoopIterationListener listener) {
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			try {
				delegate.removeIterationListener(listener);
			}
			catch (Exception e) {
				logger.warn("{}: removeIterationListener(listener) failure", getName(), e);
			}
		}
	}

	@Override
	public void testEnded() {
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			try {
				listener.testEnded();
			}
			catch (Exception e) {
				logger.warn("{}: testEnded() failure", getName(), e);
			}
		}
		destroyDelegate();
	}

	@Override
	public void testEnded(String host) {
		TestStateListener listener = getDelegateAs(TestStateListener.class);
		if (null != listener) {
			try {
				listener.testEnded(host);
			}
			catch (Exception e) {
				logger.warn("{}: testEnded(String) failure", getName(), e);
			}
		}
		destroyDelegate();
	}

	protected void destroyDelegate() {
		monitor.enter();
		try {
			DisposableBean disposable = getDelegateAs(DisposableBean.class);
			if (null != disposable) {
				try {
					disposable.destroy();
				}
				catch (Exception e) {
					logger.warn("{}: destroy() failure", getName(), e);
				}
			}
		}
		finally {
			delegateRef.set(null);
			monitor.leave();
		}
	}

	@Override
	public void setTemporary(JMeterProperty property) {
		super.setTemporary(property);
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			delegate.setProperty(property);
			delegate.setTemporary(property);
		}
	}

	@Override
	public void setRunningVersion(boolean runningVersion) {
		super.setRunningVersion(runningVersion);
		Controller delegate = delegateRef.get();
		if (null != delegate) {
			delegate.setRunningVersion(runningVersion);
		}
	}
}
