/*
Copyright 2018 Penny Rohr Curich

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

package guru.qas.martini.jmeter.controller;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings({"WeakerAccess", "unused"})
public class TagResourceBundle extends ResourceBundle {

	protected final ResourceBundle delegate;
	protected final Set<String> tags;

	protected TagResourceBundle(ResourceBundle delegate, Iterable<String> tags) {
		this.delegate = delegate;
		this.tags = null == tags ? ImmutableSet.of() : Sets.newLinkedHashSet(tags);
	}

	@Override
	public String getBaseBundleName() {
		return delegate.getBaseBundleName();
	}

	@Override
	public Locale getLocale() {
		return super.getLocale();
	}

	@Override
	protected void setParent(ResourceBundle parent) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(@Nonnull String key) {
		checkNotNull(key, "null String");
		return delegate.containsKey(key) || tags.contains(key);
	}

	@Nonnull
	@Override
	public Set<String> keySet() {
		LinkedHashSet<String> set = Sets.newLinkedHashSet(tags);
		Iterators.addAll(set, delegate.getKeys().asIterator());
		return set;
	}

	@Override
	protected Object handleGetObject(@Nonnull String key) {
		return tags.contains(key) ? key : delegate.getObject(key);
	}

	@Nonnull
	@Override
	protected Set<String> handleKeySet() {
		return keySet();
	}

	@Nonnull
	@Override
	public Enumeration<String> getKeys() {
		Set<String> keys = keySet();
		Iterator<String> iterator = keys.iterator();
		return Iterators.asEnumeration(iterator);
	}
}
