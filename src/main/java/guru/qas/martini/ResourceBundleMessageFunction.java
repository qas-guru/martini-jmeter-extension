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

package guru.qas.martini;

import java.beans.BeanDescriptor;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.annotation.Nonnull;

import org.apache.jmeter.testbeans.BeanInfoSupport;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class ResourceBundleMessageFunction implements Function<String, String> {

	protected final ResourceBundle bundle;

	protected ResourceBundleMessageFunction(@Nonnull ResourceBundle bundle) {
		this.bundle = checkNotNull(bundle, "null ResourceBundle");
	}

	@Override
	public String apply(String key) {
		checkNotNull(key, "null String");
		String trimmed = bundle.containsKey(key) ? bundle.getString(key).trim() : "";
		return trimmed.isEmpty() ? key : trimmed;
	}

	public static ResourceBundleMessageFunction getInstance(@Nonnull BeanInfoSupport s) {
		checkNotNull(s, "null BeanInfoSupport");
		BeanDescriptor descriptor = s.getBeanDescriptor();
		return getInstance(descriptor);
	}

	public static ResourceBundleMessageFunction getInstance(@Nonnull BeanDescriptor d) {
		checkNotNull(d, "null BeanDescriptor");
		Object o = d.getValue(BeanInfoSupport.RESOURCE_BUNDLE);
		ResourceBundle bundle = ResourceBundle.class.cast(o);
		return new ResourceBundleMessageFunction(bundle);
	}
}
