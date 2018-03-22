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

package guru.qas.martini.jmeter.processor;

import java.util.ResourceBundle;

import org.apache.jmeter.util.JMeterUtils;

import static java.util.ResourceBundle.getBundle;

public enum OnError {
	STOP_TEST("on.error.stop.test"),
	STOP_THREAD_GROUP("on.error.stop.thread.group"),
	STOP_THREAD("on.error.stop.thread"),
	PROCEED("on.error.proceed");

	private final ResourceBundle messageBundle;
	private final String key;

	OnError(String key) {
		messageBundle = getBundle(getClass().getName(), JMeterUtils.getLocale(), getClass().getClassLoader());
		this.key = key;
	}

	public String getLabel() {
		return messageBundle.getString(key);
	}
}