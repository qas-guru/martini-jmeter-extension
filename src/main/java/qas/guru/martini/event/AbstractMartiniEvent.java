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
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;

import com.google.common.base.MoreObjects;

import guru.qas.martini.Martini;
import guru.qas.martini.event.MartiniEvent;

@SuppressWarnings("WeakerAccess")
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

	protected AbstractMartiniEvent(long timestamp, JMeterContext context) {
		this.timestamp = timestamp;
		this.context = context;
	}

	@Override
	public Martini getMartini() {
		return null;
	}

	protected MoreObjects.ToStringHelper getThreadGroupStringHelper() {
		AbstractThreadGroup threadGroup = context.getThreadGroup();
		return null == threadGroup ? null : MoreObjects.toStringHelper(threadGroup).add("name", threadGroup.getName());
	}

	protected MoreObjects.ToStringHelper getJMeterContextStringHelper() {
		return MoreObjects.toStringHelper(context)
			.add("threadGroup", getThreadGroupStringHelper());
	}

	protected MoreObjects.ToStringHelper getStringHelper() {
		return MoreObjects.toStringHelper(this)
			.add("timestamp", timestamp)
			.add("context", getJMeterContextStringHelper());
	}

	@Override
	public String toString() {
		return getStringHelper().toString();
	}
}
