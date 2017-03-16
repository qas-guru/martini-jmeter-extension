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

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import guru.qas.martini.Mixologist;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements Serializable {

	@Override
	public void initialize() {
		super.initialize();
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();


		Object o = variables.getObject("applicationContext");
		ClassPathXmlApplicationContext context = ClassPathXmlApplicationContext.class.cast(o);
		context.getBean(Mixologist.class);
		System.out.println("breakpoint");

		// TODO FOR EACH MARTINI: this.addTestElement(child);
		// set our martinis and monitors from variables, or instantiate
	}

	@Override
	public Sampler next() {
		System.out.println("breakpoint");
		return super.next();
	}
}
