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

package guru.qas.martini.jmeter;

import javax.annotation.Nullable;

import org.apache.jmeter.gui.GuiPackage;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.cal10n.LocLogger;

import com.google.common.base.Throwables;

@SuppressWarnings("WeakerAccess")
public class DefaultExceptionReporter implements ExceptionReporter {

	protected final Logger defaultLogger;
	protected final LocLogger logger;

	public DefaultExceptionReporter() {
		this(null);
	}

	public DefaultExceptionReporter(@Nullable LocLogger logger) {
		this.logger = logger;
		Class<? extends DefaultExceptionReporter> implementation = this.getClass();
		defaultLogger = LoggerFactory.getLogger(implementation);
	}

	@Override
	public void logException(Enum<?> key, Exception e, Object... arguments) {
		execute(e, () -> {
			String message = Messages.getMessage(key, arguments);
			logger.error(message, e);
		});
	}

	@Override
	public void showException(Enum<?> key, Exception e, Object... arguments) {
		execute(e, () -> {
			if (null != GuiPackage.getInstance()) {
				String stacktrace = Throwables.getStackTraceAsString(e);
				String title = Messages.getMessage(key, arguments);
				JMeterUtils.reportErrorToUser(stacktrace, title, e);
			}
		});
	}

	protected void execute(Exception original, Runnable r) {
		try {
			r.run();
		}
		catch (Exception e) {
			defaultLogger.warn("encountered exception while reporting", e);
			defaultLogger.error("original exception:\n", original);
		}
	}
}
