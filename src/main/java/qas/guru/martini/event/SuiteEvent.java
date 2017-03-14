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

import org.apache.jmeter.threads.JMeterContext;

import guru.qas.martini.Martini;
import guru.qas.martini.event.MartiniEvent;

@SuppressWarnings("WeakerAccess")
public class SuiteEvent implements MartiniEvent {

	public enum Type {
		ABORTED, STARTED, ENDED
	}

	private final long timestamp;
	private final JMeterContext context;
	private final Type type;

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public Martini getMartini() {
		return null;
	}

	public JMeterContext getContext() {
		return context;
	}

	public Type getType() {
		return type;
	}

	protected SuiteEvent(long timestamp, JMeterContext context, Type type) {
		this.timestamp = timestamp;
		this.context = context;
		this.type = type;
	}

	public static SuiteEvent getStarted(JMeterContext context) {
		long now = System.currentTimeMillis();
		return new SuiteEvent(now, context, Type.STARTED);
	}

	public static SuiteEvent getEnded(JMeterContext context) {
		long now = System.currentTimeMillis();
		return new SuiteEvent(now, context, Type.ENDED);
	}

	public static SuiteEvent getAborted(JMeterContext context) {
		long now = System.currentTimeMillis();
		return new SuiteEvent(now, context, Type.ABORTED);
	}
}
