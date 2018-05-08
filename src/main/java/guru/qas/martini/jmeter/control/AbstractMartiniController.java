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

import org.apache.jmeter.control.Controller;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.TestCompilerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.qas.martini.jmeter.SpringBeanUtil;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public abstract class AbstractMartiniController extends AbstractTestElement implements Controller, Serializable, TestStateListener, TestCompilerHelper, LoopIterationListener {

	private static final long serialVersionUID = -3785811213682702141L;

	protected transient Logger logger;
	protected transient Controller delegate;
	protected transient TestStateListener asTestStateListener;
	protected transient LoopIterationListener asLoopIterationListener;
	protected transient TestCompilerHelper asTestCompilerHelper;

	public AbstractMartiniController() {
		super();
		logger = LoggerFactory.getLogger(this.getClass());
	}

	@Override
	public Object clone() {
		AbstractMartiniController clone = AbstractMartiniController.class.cast(super.clone());
		if (NoThreadClone.class.isInstance(delegate)) {
			clone.cast(delegate);
		}
		else if (Cloneable.class.isInstance(delegate)) {
			clone.cast(delegate.clone());
		}
		return clone;
	}

	protected void cast(Object o) {
		delegate = Controller.class.isInstance(o) ? Controller.class.cast(o) : null;
		asTestStateListener = TestStateListener.class.isInstance(o) ? TestStateListener.class.cast(o) : null;
		asLoopIterationListener = LoopIterationListener.class.isInstance(o) ? LoopIterationListener.class.cast(o) : null;
		asTestCompilerHelper = TestCompilerHelper.class.isInstance(o) ? TestCompilerHelper.class.cast(o) : null;
	}

	@Override
	public void testStarted() {
		initializeDelegate();
		if (null != asTestStateListener) {
			asTestStateListener.testStarted();
		}
	}

	protected void initializeDelegate() {
		cast(checkNotNull(createDelegate()));
	}

	protected abstract Controller createDelegate();

	@Override
	public void addIterationListener(LoopIterationListener listener) {
		if (null != delegate) {
			delegate.addIterationListener(listener);
		}
	}

	@Override
	public void addTestElement(TestElement element) {
		if (null != asTestCompilerHelper) {
			asTestCompilerHelper.addTestElementOnce(element);
		}
	}

	@Override
	public boolean addTestElementOnce(TestElement child) {
		boolean evaluation = false;
		if (null != asTestCompilerHelper) {
			evaluation = asTestCompilerHelper.addTestElementOnce(child);
		}
		else if (null != delegate) {
			evaluation = true;
			delegate.addTestElement(child);
		}
		return evaluation;
	}

	@Override
	public void testStarted(String host) {
		initializeDelegate();
		if (null != asTestStateListener) {
			asTestStateListener.testStarted(host);
		}
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		if (null != asLoopIterationListener) {
			asLoopIterationListener.iterationStart(event);
		}
	}

	@Override
	public void initialize() {
		if (null != delegate) {
			delegate.initialize();
		}
	}

	@Override
	public Sampler next() {
		return null == delegate ? null : delegate.next();
	}

	@Override
	public boolean isDone() {
		return null == delegate || delegate.isDone();
	}

	@Override
	public void triggerEndOfLoop() {
		if (null != delegate) {
			delegate.triggerEndOfLoop();
		}
	}

	@Override
	public void removeIterationListener(LoopIterationListener listener) {
		if (null != delegate) {
			delegate.removeIterationListener(listener);
		}
	}

	@Override
	public void testEnded() {
		try {
			if (null != asTestStateListener) {
				asTestStateListener.testEnded();
			}
		}
		finally {
			releaseMembers();
		}
	}

	@Override
	public void testEnded(String host) {
		try {
			if (null != asTestStateListener) {
				asTestStateListener.testEnded(host);
			}
		}
		finally {
			releaseMembers();
		}
	}

	protected void releaseMembers() {
		SpringBeanUtil.destroy(getName(), delegate);
		delegate = null;
		asTestStateListener = null;
		asLoopIterationListener = null;
	}
}
