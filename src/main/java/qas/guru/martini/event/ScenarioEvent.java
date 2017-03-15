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

import guru.qas.martini.Martini;
import guru.qas.martini.event.MartiniEvent;

public class ScenarioEvent implements MartiniEvent {

	public enum Type {
		STARTING, ENDED
	}

	private final long timestamp;
	private final JMeterContext context;
	private final Martini martini;
	private final Type type;

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	public JMeterContext getJmeterContext() {
		return context;
	}

	@Override
	public Martini getMartini() {
		return martini;
	}

	public Type getType() {
		return type;
	}

	private ScenarioEvent(long timestamp, JMeterContext context, Martini martini, Type type) {
		this.timestamp = timestamp;
		this.context = context;
		this.martini = martini;
		this.type = type;
	}

	public static ScenarioEvent getStarting(JMeterContext context, Martini martini) {
		long now = System.currentTimeMillis();
		return new ScenarioEvent(now, context, martini, Type.STARTING);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("type", type)
			.add("martini", martini)
			.toString();
	}
}
