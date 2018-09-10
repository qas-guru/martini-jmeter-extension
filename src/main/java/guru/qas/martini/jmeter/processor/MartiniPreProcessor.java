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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.processor.PreProcessor;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.testelement.TestStateListener;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import com.google.common.collect.ImmutableList;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.JMeterContextUtil;

@SuppressWarnings("WeakerAccess")
public final class MartiniPreProcessor extends AbstractTestElement implements NoThreadClone, PreProcessor, TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 6143536078921717477L;

	public static final String DEFAULT_SPEL_FILTER = "!isWIP()";

	protected String spelFilter;
	protected boolean shuffled;
	protected String randomSeed;

	protected transient MessageSource messageSource;
	protected transient Logger logger;

	protected transient List<Martini> martinis;
	protected transient String variableKey;
	protected transient ConcurrentHashMap<Integer, ConcurrentLinkedDeque<Martini>> dequeIndex;

	public String getSpelFilter() {
		return spelFilter;
	}

	public void setSpelFilter(String spelFilter) {
		this.spelFilter = spelFilter;
	}

	public boolean isShuffled() {
		return shuffled;
	}

	public void setShuffled(boolean shuffled) {
		this.shuffled = shuffled;
	}

	public String getRandomSeed() {
		return randomSeed;
	}

	public void setRandomSeed(String randomSeed) {
		this.randomSeed = randomSeed;
	}

	public MartiniPreProcessor() {
		super();
		init();
	}

	protected void init() {
		Class<? extends MartiniPreProcessor> implementation = getClass();
		messageSource = MessageSources.getMessageSource(implementation);
		logger = LoggerFactory.getLogger(getClass());
		variableKey = implementation.getName();
		dequeIndex = new ConcurrentHashMap<>();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	public void testStarted(String host) {
		testStarted();
	}

	public void testStarted() {
		try {
			ApplicationContext applicationContext = JMeterContextUtil.getVariable(ApplicationContext.class)
				.orElseThrow(() -> new IllegalStateException("no ApplicationContext variable set"));

			Mixologist mixologist = applicationContext.getBean(Mixologist.class);

			String filter = this.getNormalizedSpelFilter().orElse(null);
			Collection<Martini> collection = null == filter ? mixologist.getMartinis() : mixologist.getMartinis(filter);

			if (isShuffled()) {
				List<Martini> asList = new ArrayList<>(collection);
				Random random = getRandom();
				Collections.shuffle(asList, random);
				this.martinis = ImmutableList.copyOf(asList);
			}
			else {
				this.martinis = ImmutableList.copyOf(collection);
			}
		}
		catch (Exception e) {
			StandardJMeterEngine.stopEngineNow();
			MartiniException martiniException = new MartiniException.Builder()
				.setLocale(JMeterUtils.getLocale())
				.setMessageSource(messageSource)
				.setKey("error.retrieving.spring.context")
				.setCause(e)
				.build();
			logger.error(martiniException.getMessage(), e);
			Gui.reportError(this, martiniException);
		}
	}

	protected Optional<String> getNormalizedSpelFilter() {
		String filter = getSpelFilter();
		String trimmed = null == filter ? "" : filter.trim();
		return trimmed.isEmpty() ? Optional.empty() : Optional.of(trimmed);
	}

	protected Random getRandom() {
		String seed = getRandomSeed();
		String trimmed = null == seed ? "" : seed.trim();

		SecureRandom random = new SecureRandom();
		if (trimmed.isEmpty()) {
			random.setSeed(System.currentTimeMillis());
		}
		else {
			random.setSeed(trimmed.getBytes());
		}
		return random;
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		int iteration = event.getIteration();

		ConcurrentLinkedDeque<Martini> deque =
			dequeIndex.computeIfAbsent(iteration, i -> new ConcurrentLinkedDeque<>(this.martinis));

		JMeterContext context = JMeterContextService.getContext();
		JMeterVariables variables = context.getVariables();
		variables.putObject(variableKey, deque);
	}

	public void process() {
		JMeterContext context = JMeterContextService.getContext();
		JMeterVariables variables = context.getVariables();
		Object o = variables.getObject(variableKey);

		if (ConcurrentLinkedDeque.class.isInstance(o)) {
			ConcurrentLinkedDeque cast = ConcurrentLinkedDeque.class.cast(o);
			Object polled = cast.poll();
			Sampler currentSampler = context.getCurrentSampler();
			System.out.println("CURRENT SAMPLER: " + currentSampler);
			if (null != currentSampler) {
				JMeterContextUtil.setSamplerData(currentSampler, polled, Martini.class);
			}
		}
	}

	public void testEnded() {
		this.martinis = null;
		this.dequeIndex = null;
	}

	public void testEnded(String host) {
		testEnded();
	}
}
