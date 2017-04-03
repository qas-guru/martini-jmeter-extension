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

package guru.qas.martini.jmeter.sampler;

import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.jmeter.threads.JMeterContext;

import com.google.common.collect.ImmutableList;

import guru.qas.martini.event.Status;

@SuppressWarnings("WeakerAccess")
public class SamplerContext {

	protected static final String KEY_STATUS = "martini.sampler.status";
	protected static final String KEY_EXCEPTION = "martini.sampler.exception";
	protected static final String KEY_ENTITY = "martini.sampler.http.entities";

	protected final JMeterContext context;

	protected SamplerContext(JMeterContext context) {
		this.context = context;
	}

	protected void clear() {
		remove(KEY_STATUS);
		remove(KEY_EXCEPTION);
		remove(KEY_ENTITY);
	}

	protected void remove(String key) {
		Map<String, Object> samplerContext = context.getSamplerContext();
		samplerContext.remove(key);
	}

	protected void setStatus(Status s) {
		set(KEY_STATUS, s);
	}

	protected void set(String key, Object o) {
		Map<String, Object> samplerContext = context.getSamplerContext();
		samplerContext.put(key, o);
	}

	protected Status getStatus() {
		return get(KEY_STATUS, Status.class);
	}

	protected <T> T get(String key, Class<? extends T> implementation) {
		Map<String, Object> samplerContext = context.getSamplerContext();
		Object o = samplerContext.get(key);
		return implementation.isInstance(o) ? implementation.cast(o) : null;
	}

	protected void setException(Exception e) {
		set(KEY_EXCEPTION, e);
	}

	protected Exception getException() {
		return get(KEY_EXCEPTION, Exception.class);
	}

	protected void setHttpEntities(Iterable<HttpEntity> i) {
		set(KEY_ENTITY, ImmutableList.copyOf(i));
	}

	@SuppressWarnings("unchecked")
	protected ImmutableList<HttpEntity> getHttpEntities() {
		return get(KEY_ENTITY, ImmutableList.class);
	}
}
