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
import java.util.Collection;
import java.util.Iterator;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.NextIsNullException;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.sampler.DebugSampler;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.util.JMeterStopTestException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Monitor;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements Serializable {

	protected transient final Monitor monitor;
	protected transient ImmutableList<Martini> martinis;
	protected transient Iterator<Martini> iterator;

	public MartiniController() {
		super();
		this.monitor = new Monitor();
	}

	@Override
	public void initialize() {
		super.initialize();
		JMeterVariables variables = getJMeterVariables();
		initialize(variables);
	}

	private JMeterVariables getJMeterVariables() {
		JMeterContext threadContext = super.getThreadContext();
		return threadContext.getVariables();
	}

	protected void initialize(JMeterVariables variables) {
		Object o = variables.getObject("applicationContext"); // TODO: a constant
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);
		Mixologist mixologist = context.getBean(Mixologist.class);
		initialize(mixologist);
	}

	protected void initialize(Mixologist mixologist) {
		Collection<Martini> martinis = mixologist.getMartinis(); // TODO: scenario filtering
		if (martinis.isEmpty()) {
			String message = String.format("%s:%s has no scenarios to run", getClass().getName(), getName());
			throw new JMeterStopTestException(message);
		}
		initialize(martinis);
	}

	protected void initialize(Collection<Martini> martinis) {
		this.martinis = ImmutableList.copyOf(martinis);
		iterator = martinis.iterator();
	}

	@Override
	protected void reInitialize() {
		super.reInitialize();
		iterator = martinis.iterator();
	}


	@Override
	protected TestElement getCurrentElement() throws NextIsNullException {
		TestElement currentElement = super.getCurrentElement();
		System.out.println("breakpoint");
		return currentElement;
	}

	@Override
	public boolean isDone() {
		monitor.enter();
		try {
			return !iterator.hasNext();
		}
		finally {
			monitor.leave();
		}
	}
}