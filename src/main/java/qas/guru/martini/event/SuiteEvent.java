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

import com.google.common.base.MoreObjects;

@SuppressWarnings("WeakerAccess")
public class SuiteEvent extends AbstractMartiniEvent {

	public enum Type {
		STARTING, ENDED
	}

	private final Type type;

	public Type getType() {
		return type;
	}

	protected SuiteEvent(long timestamp, JMeterContext context, Type type) {
		super(timestamp, context);
		this.type = type;
	}

	public static SuiteEvent getStarting(JMeterContext context) {
		long now = System.currentTimeMillis();
		return new SuiteEvent(now, context, Type.STARTING);
	}

	public static SuiteEvent getEnded(JMeterContext context) {
		long now = System.currentTimeMillis();
		return new SuiteEvent(now, context, Type.ENDED);
	}

	@Override
	protected MoreObjects.ToStringHelper getStringHelper() {
		return MoreObjects.toStringHelper(this)
			.add("timestamp", getTimestamp())
			.add("type", type)
			.add("context", getJMeterContextStringHelper());
	}
}
