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

package guru.qas.martini.jmeter.preprocessor;

import java.io.Serializable;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.springframework.context.ApplicationContext;

/**
 * Creates and destroys a Spring application context on test start and end, making the ApplicationContext
 * accessible through variable martini.spring.application.context to setup and test threads.
 */
@SuppressWarnings("WeakerAccess")
public class SpringPreProcessor
	extends AbstractTestElement
	implements Serializable, Cloneable, PreProcessor, TestBean, TestStateListener, TestIterationListener {

	private static final long serialVersionUID = -1582951167073002597L;

	public static final String VARIABLE = "martini.spring.application.context";

	// Shared.
	protected transient ApplicationContext springContext;

	public SpringPreProcessor() {
		super();
	}

	@Override
	public void testStarted() { // todo: load
		System.out.println("testStarted");
	}

	@Override
	public void testStarted(String host) {
		System.out.println("testStarted(String)");
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		SpringPreProcessor clone = SpringPreProcessor.class.cast(o);
		clone.springContext = springContext;
		return clone;
	}

	@Override
	public void process() {
		System.out.println("process()");
	}

	@Override
	public void testIterationStart(LoopIterationEvent event) {
		System.out.println("testIterationStart(LoopIterationEvent)");
	}

	@Override
	public void testEnded() { // todo: nullify
		System.out.println("testEnded()");
	}

	@Override
	public void testEnded(String host) {
		System.out.println("testEnded(String)");
	}
}
