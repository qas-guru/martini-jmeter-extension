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

package guru.qas.martini.report;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

@SuppressWarnings("WeakerAccess")
public class Sample {

	protected Long timestamp;
	protected Long timeElapsed;
	protected String json;
	protected ImmutableList<Sample> subSamples;

	protected Sample(Long timestamp, Long timeElapsed, String json, ImmutableList<Sample> subSamples) {
		this.timestamp = timestamp;
		this.timeElapsed = timeElapsed;
		this.json = json;
		this.subSamples = subSamples;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
			.add("timestamp", timestamp)
			.add("timeElapsed", timeElapsed)
			.add("json", json)
			.add("subSamples", Joiner.on(",").skipNulls().join(subSamples))
			.toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		protected String timeElapsed;
		protected String timestamp;
		protected String json;
		protected List<Sample> subSamples;

		protected Builder() {
			this.subSamples = Lists.newArrayList();
		}

		public Builder setTimeElapsed(String s) {
			this.timeElapsed = s;
			return this;
		}

		public Builder setTimestamp(String s) {
			this.timestamp = s;
			return this;
		}

		public Builder setResponseData(String s) {
			this.json = s;
			return this;
		}

		public Builder addSub(Sample sample) {
			subSamples.add(sample);
			return this;
		}

		public Sample build() {
			Long timestampAsLong = null == timestamp ? null : Long.valueOf(timestamp);
			Long interval = null == timeElapsed ? null : Long.valueOf(timeElapsed);
//			JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
//			System.out.println("breakpoint");
			return new Sample(timestampAsLong, interval, json, ImmutableList.copyOf(subSamples));
		}
	}
}
