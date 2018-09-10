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

package guru.qas.martini.jmeter.control;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import com.google.common.collect.Iterables;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.Gui;
import guru.qas.martini.jmeter.JMeterContextUtil;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements TestStateListener, LoopIterationListener {

	protected static final String PROPERTY_SPEL_FILTER = "spel.filter";
	protected static final String PROPERTY_SHUFFLED = "shuffled.martinis";
	protected static final String PROPERTY_RANDOM_SEED = "random.seed";

	public static final String DEFAULT_SPEL_FILTER = "!isWIP()";
	public static final boolean DEFAULT_SHUFFLED = false;
	public static final String DEFAULT_RANDOM_SEED = null;

	protected transient MessageSource messageSource;
	protected transient Logger logger;
	protected transient List<Martini> martinis;
	protected transient ConcurrentHashMap<Integer, ConcurrentLinkedDeque<Martini>> dequeIndex;

	protected transient ConcurrentLinkedDeque<Martini> deque;

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	public void setSpelFilter(String spelFilter) {
		super.setProperty(PROPERTY_SPEL_FILTER, spelFilter);
	}

	public boolean isShuffled() {
		return super.getPropertyAsBoolean(PROPERTY_SHUFFLED, DEFAULT_SHUFFLED);
	}

	public void setShuffled(boolean shuffled) {
		super.setProperty(PROPERTY_SHUFFLED, shuffled);
	}

	public String getRandomSeed() {
		return super.getPropertyAsString(PROPERTY_RANDOM_SEED);
	}

	public void setRandomSeed(String randomSeed) {
		super.setProperty(PROPERTY_RANDOM_SEED, randomSeed);
	}

	public MartiniController() {
		super();
		init();
		dequeIndex = new ConcurrentHashMap<>();
	}

	protected void init() {
		Class<? extends MartiniController> implementation = getClass();
		messageSource = MessageSources.getMessageSource(implementation);
		logger = LoggerFactory.getLogger(getClass());
		martinis = new ArrayList<>();
		dequeIndex = new ConcurrentHashMap<>();
	}

	protected Object readResolve() {
		init();
		return this;
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniController clone = MartiniController.class.cast(o);
		clone.martinis = martinis;
		clone.dequeIndex = dequeIndex;
		return clone;
	}

	@Override
	public void testStarted() {
		try {
			ApplicationContext applicationContext = JMeterContextUtil.getVariable(ApplicationContext.class)
				.orElseThrow(() -> new IllegalStateException("no ApplicationContext variable set"));
			Mixologist mixologist = applicationContext.getBean(Mixologist.class);

			String filter = this.getNormalizedSpelFilter().orElse(null);
			Collection<Martini> collection = null == filter ? mixologist.getMartinis() : mixologist.getMartinis(filter);
			Iterables.addAll(martinis, collection);

			if (isShuffled()) {
				Random random = getRandom();
				Collections.shuffle(martinis, random);
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
	public void testStarted(String host) {
		testStarted();
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		int iteration = event.getIteration();
		deque = dequeIndex.computeIfAbsent(iteration, i -> new ConcurrentLinkedDeque<>(this.martinis));
	}

	@Override
	public Sampler next() {

		Sampler sampler = null;
		try {
			Martini martini = deque.poll();

			if (null == martini) {
				super.getThreadContext().setStartNextThreadLoop(true);
			}
			else {
				sampler = super.next();
				if (null == sampler) {
					super.initializeSubControllers();
					sampler = super.next();
				}
			}

			if (null != sampler) {
				JMeterContextUtil.setSamplerData(sampler, martini, Martini.class);
			}

			return sampler;
		}
		catch (MartiniException e) {
			StandardJMeterEngine.stopEngineNow();
			logger.error(e.getMessage(), e);
			Gui.reportError(this, e);
		}
		catch (Exception e) {
			StandardJMeterEngine.stopEngineNow();
			MartiniException martiniException = new MartiniException.Builder()
				.setLocale(JMeterUtils.getLocale())
				.setMessageSource(messageSource)
				.setKey("error.retrieving.martini")
				.setCause(e)
				.build();
			logger.error(martiniException.getMessage(), e);
			Gui.reportError(this, martiniException);
		}

		return sampler;
	}

	@Override
	public void testEnded() {
		deque = null;
		dequeIndex = null;
		martinis = null;
		logger = null;
		messageSource = null;
	}

	@Override
	public void testEnded(String host) {
		testEnded();
	}
}