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

package qas.guru.martini.event;

import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;

import com.google.common.base.MoreObjects;

import guru.qas.martini.event.MartiniEvent;

public abstract class AbstractMartiniEvent implements MartiniEvent {

	private final long timestamp;
	private final JMeterContext context;

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	public JMeterContext getJMeterContext() {
		return context;
	}

	AbstractMartiniEvent(long timestamp, JMeterContext context) {
		this.timestamp = timestamp;
		this.context = context;
	}

	protected String getContextSummary() {
		Sampler sampler = context.getCurrentSampler();
		JMeterThread thread = context.getThread();

		return MoreObjects
			.toStringHelper(context)
			.add("currentSampler", sampler.getName())
			.add("thread", thread.getThreadName())
			.toString();
	}
}
