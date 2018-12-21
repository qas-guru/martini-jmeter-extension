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


import javax.annotation.Nullable;

import org.slf4j.cal10n.LocLogger;
import org.slf4j.cal10n.LocLoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;

import ch.qos.cal10n.IMessageConveyor;
import guru.qas.martini.Messages;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.runtime.event.EventManager;

import static com.google.common.base.Preconditions.checkNotNull;
import static guru.qas.martini.jmeter.controller.DefaultMartiniScopeControllerBeanMessages.*;

@SuppressWarnings("WeakerAccess")
@Configurable
public class DefaultMartiniScopeControllerBean
	implements InitializingBean, MartiniScopeControllerBean {

	private final EventManager eventManager;
	private final ThreadLocal<MartiniResult> threadLocal;
	private LocLogger logger;

	protected DefaultMartiniScopeControllerBean(EventManager eventManager) {
		this.eventManager = eventManager;
		threadLocal = new ThreadLocal<>();
	}

	@Override
	public void afterPropertiesSet() {
		setUpLogger();
	}

	protected void setUpLogger() {
		IMessageConveyor messageConveyor = Messages.getMessageConveyor();
		LocLoggerFactory factory = new LocLoggerFactory(messageConveyor);
		logger = factory.getLocLogger(this.getClass());
	}

	@Override
	public void publishBeforeScenario(MartiniResult result) {
		checkNotNull(result, "null MartiniResult");

		MartiniResult previous = threadLocal.get();
		if (result.equals(previous)) {
			logger.warn(BEFORE_EVENT_ALREADY_PUBLISHED, result);
		}
		else {
			logger.debug(PUBLISHING_BEFORE_EVENT, result);
			threadLocal.set(result);
			eventManager.publishBeforeScenario(this, result);
		}
	}

	@Override
	public void publishAfterScenario(@Nullable MartiniResult result) {

		MartiniResult previous = threadLocal.get();
		threadLocal.remove();

		if (null != previous && previous.equals(result)) {
			logger.debug(PUBLISHING_AFTER_EVENT, result);
			eventManager.publishAfterScenario(this, result);
		}
		else if (null != previous && null != result && !previous.equals(result)) {
			logger.warn(AFTER_EVENT_NOT_PUBLISHED, previous);
			logger.debug(PUBLISHING_AFTER_EVENT, previous);
			eventManager.publishAfterScenario(this, previous);
			logger.debug(PUBLISHING_AFTER_EVENT, result);
			eventManager.publishAfterScenario(this, result);
		}
		else if (null == previous && null != result) {
			logger.warn(BEFORE_EVENT_NOT_PUBLISHED, result);
			eventManager.publishAfterScenario(this, result);
		}
	}

	@Override
	public void destroy() {
		MartiniResult current = threadLocal.get();
		publishAfterScenario(current);
	}
}
