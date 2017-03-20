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

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;

import com.google.common.base.MoreObjects;

import guru.qas.martini.Martini;

@SuppressWarnings("WeakerAccess")
public class ScenarioEvent extends AbstractMartiniEvent {

	public enum Type {
		STARTING, ENDED
	}

	private final SampleResult result;
	private final Type type;

	public SampleResult getResult() {
		return result;
	}

	public Type getType() {
		return type;
	}

	protected ScenarioEvent(long timestamp, JMeterContext context, Type type, Martini martini, SampleResult result) {
		super(timestamp, context, martini);
		this.type = type;
		this.result = result;
	}

	public static ScenarioEvent getStarting(JMeterContext context, Martini martini) {
		long now = System.currentTimeMillis();
		return new ScenarioEvent(now, context, Type.STARTING, martini, null);
	}

	public static ScenarioEvent getEnded(JMeterContext context, Martini martini, SampleResult result) {
		long timestamp = result.getEndTime();
		return new ScenarioEvent(timestamp, context, Type.ENDED, martini, result);
	}

	@Override
	protected MoreObjects.ToStringHelper getStringHelper() {
		return MoreObjects.toStringHelper(this)
			.add("timestamp", super.getTimestamp())
			.add("type", type)
			.add("context", getJMeterContextStringHelper())
			.add("martini", super.getMartini())
			.add("result", result);
	}
}
