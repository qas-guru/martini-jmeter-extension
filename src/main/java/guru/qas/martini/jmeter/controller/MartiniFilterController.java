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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testbeans.TestBean;
import org.apache.jmeter.testelement.TestStateListener;
import org.springframework.context.ApplicationContext;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Striped;

import guru.qas.martini.Martini;
import guru.qas.martini.Mixologist;
import guru.qas.martini.jmeter.SamplerContext;
import guru.qas.martini.jmeter.Variables;

import static com.google.common.base.Preconditions.*;
import static guru.qas.martini.jmeter.controller.MartiniFilterControllerMessages.*;

/**
 * Selects Martinis to execute, iterating over all matching Martinis per iteration.
 */
@SuppressWarnings("WeakerAccess")
public class MartiniFilterController extends AbstractGenericController
	implements Serializable, Cloneable, TestBean, TestStateListener, LoopIterationListener {

	private static final long serialVersionUID = 4631820992406669501L;

	// These must match field names exactly.
	protected static final String PROPERTY_NO_MARTINI_FOUND_FATAL = "noMartiniFoundFatal";
	protected static final String PROPERTY_UNIMPLEMENTED_STEPS_FATAL = "unimplementedStepsFatal";
	protected static final String PROPERTY_SPEL_FILTER = "spelFilter";
	protected static final String PROPERTY_SHUFFLE = "shuffle";
	protected static final String PROPERTY_RANDOM_SEED = "randomSeed";

	// Serialized.
	protected boolean noMartiniFoundFatal;
	protected boolean unimplementedStepsFatal;
	protected String spelFilter;
	protected boolean shuffle;
	protected Long randomSeed;

	// Shared
	protected transient ImmutableList<Martini> martinis;
	protected transient volatile ConcurrentHashMap<Integer, Iterator<Martini>> index;
	protected transient volatile Striped<Lock> striped;

	// Per-thread.
	protected transient Martini martini;

	public boolean isNoMartiniFoundFatal() {
		return noMartiniFoundFatal;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setNoMartiniFoundFatal(boolean noMartinisFoundFatal) {
		this.noMartiniFoundFatal = noMartinisFoundFatal;
	}

	public boolean isUnimplementedStepsFatal() {
		return unimplementedStepsFatal;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setUnimplementedStepsFatal(boolean unimplementedStepsFatal) {
		this.unimplementedStepsFatal = unimplementedStepsFatal;
	}

	public String getSpelFilter() {
		return spelFilter;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setSpelFilter(String s) {
		spelFilter = null == s ? "" : s.trim();
	}

	public boolean isShuffle() {
		return shuffle;
	}

	public Long getRandomSeed() {
		return randomSeed;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setRandomSeed(Long l) {
		this.randomSeed = l;
	}

	@SuppressWarnings("unused") // Accessed via introspection.
	public void setShuffle(boolean b) {
		this.shuffle = b;
	}

	public MartiniFilterController() {
		super();
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() {
		return new MartiniFilterControllerBeanInfo();
	}

	@Override
	protected void completeSetup() {
		index = new ConcurrentHashMap<>();
		striped = Striped.lock(10);

		Collection<Martini> martinis = getMartinis();
		checkState(!isNoMartiniFoundFatal() || !martinis.isEmpty(), messageConveyor.getMessage(NO_MARTINI_FOUND));

		completeSetup(martinis);
		if (martinis.isEmpty()) {
			super.setDone(true);
		}
	}

	protected Collection<Martini> getMartinis() {
		String filter = getSpelFilter();
		return null == filter || filter.trim().isEmpty() ? getAllMartinis() : getFilteredMartinis();
	}

	protected Collection<Martini> getAllMartinis() {
		Mixologist mixologist = getMixologist();
		return mixologist.getMartinis();
	}

	protected Mixologist getMixologist() {
		ApplicationContext springContext = Variables.getSpringApplicationContext();
		return springContext.getBean(Mixologist.class);
	}

	protected Collection<Martini> getFilteredMartinis() {
		String spelFilter = getSpelFilter().trim();
		Mixologist mixologist = getMixologist();
		return mixologist.getMartinis(spelFilter);
	}

	protected void completeSetup(Collection<Martini> martinis) {
		List<Martini> unimplemented = isUnimplementedStepsFatal() ? getUnimplemented(martinis) : ImmutableList.of();
		if (!unimplemented.isEmpty()) {
			String message = messageConveyor.getMessage(UNIMPLEMENTED_STEPS, '\n' + Joiner.on('\n').join(unimplemented));
			throw new IllegalStateException(message);
		}

		ImmutableList.Builder<Martini> builder = ImmutableList.builder();
		if (isShuffle()) {
			Random random = getRandom();
			List<Martini> copy = Lists.newArrayList(martinis);
			Collections.shuffle(copy, random);
			builder.addAll(copy);
		}
		else {
			builder.addAll(martinis);
		}
		this.martinis = builder.build();
	}

	protected Random getRandom() {
		Long seed = getRandomSeed();
		String message = "";
		checkNotNull(seed, message);
		return new Random(seed);
	}

	protected List<Martini> getUnimplemented(Collection<Martini> martinis) {
		return martinis.stream()
			.filter(martini -> martini.getStepIndex().values().stream()
				.map(implementation -> implementation.getMethod().orElse(null))
				.anyMatch(Objects::isNull))
			.collect(Collectors.toList());
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniFilterController clone = MartiniFilterController.class.cast(o);
		clone.index = index;
		clone.striped = striped;
		clone.martinis = martinis;
		return clone;
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		setMartini(getNextMartini());
	}

	protected void setMartini(@Nullable Martini martini) {
		this.martini = martini;
		Variables.set(martini);
		SamplerContext.set(martini);
	}

	@Override
	public Sampler next() {
		Sampler sampler = null;
		if (null != martini) {
			sampler = super.next(); // Resets children if last child exhausted.
			if (null == sampler) {
				setMartini(getNextMartini());
				sampler = null == martini ? null : super.next();
			}
		}
		return sampler;
	}

	protected Martini getNextMartini() {
		int iteration = Variables.getIteration();
		Iterator<Martini> iterator = index.computeIfAbsent(iteration, i -> martinis.iterator());

		Lock lock = striped.get(iteration);
		Martini martini = null;
		try {
			lock.lockInterruptibly();
			try {
				martini = iterator.hasNext() ? iterator.next() : null;
			}
			finally {
				lock.unlock();
			}
		}
		catch (InterruptedException e) {
			String stacktrace = Throwables.getStackTraceAsString(e);
			logger.warn(INTERRUPTED, getName(), '\n' + stacktrace);
			setDone(true);
		}
		return martini;
	}

	@Override
	protected void beginTearDown() {
		index = null;
		striped = null;
		martinis = null;
		martini = null;
	}
}
