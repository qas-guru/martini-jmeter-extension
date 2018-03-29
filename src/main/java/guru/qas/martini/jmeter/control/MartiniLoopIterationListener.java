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

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Striped;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;
import guru.qas.martini.i18n.MessageSources;
import guru.qas.martini.jmeter.JMeterContextUtil;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class MartiniLoopIterationListener extends CacheLoader<Integer, Iterator<Martini>> implements TestStateListener, LoopIterationListener {

	private final ApplicationContext context;
	private final String spelFilter;
	private final Striped<Lock> striped;
	private final AtomicReference<Collection<Martini>> martinisRef;
	private final LoadingCache<Integer, Iterator<Martini>> cache;
	private final MessageSource messageSource;
	private final Logger logger;

	public MartiniLoopIterationListener(@Nonnull ApplicationContext context, @Nullable String spelFilter) {
		checkNotNull(context, "null ApplicationContext");
		this.context = context;
		this.spelFilter = spelFilter;
		striped = Striped.lock(100);
		martinisRef = new AtomicReference<>();
		cache = CacheBuilder.newBuilder()
			.concurrencyLevel(100)
			.build(this);
		messageSource = MessageSources.getMessageSource(getClass());
		logger = LoggerFactory.getLogger(getClass());
	}

	@Override
	public void testStarted() {
		testStarted(null);
	}

	@Override
	public void testStarted(String host) {
		Locale locale = JMeterUtils.getLocale();
		try {
			Mixologist mixologist = context.getBean(Mixologist.class);
			Collection<Martini> martinis = mixologist.getMartinis(spelFilter);
			if (martinis.isEmpty()) {
				String message = null == spelFilter ?
					messageSource.getMessage("no.martinis.found", null, locale) :
					messageSource.getMessage("no.martinis.found.matching.filter", new Object[]{spelFilter}, locale);
				logger.warn(message);
			}
			martinisRef.compareAndSet(null, martinis);
		}
		catch (Exception e) {
			throw new MartiniException.Builder()
				.setCause(e)
				.setLocale(locale)
				.setMessageSource(messageSource)
				.setKey("exception.loading.martinis")
				.build();
		}
	}

	@Override
	public Iterator<Martini> load(@Nonnull Integer iteration) {
		Collection<Martini> martinis = martinisRef.get();
		Iterator<Martini> iterator = null == martinis ? ImmutableList.<Martini>of().iterator() : martinis.iterator();
		Iterator<Martini> immutable = Iterators.unmodifiableIterator(iterator);
		Lock lock = striped.get(iterator);
		return new LockingIterator<>(immutable, lock);
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		int iteration = event.getIteration();
		Iterator<Martini> i = cache.getUnchecked(iteration);
		JMeterContextUtil.setVariable(i, LockingIterator.class);
	}

	@Override
	public void testEnded() {
		testEnded(null);
	}

	@Override
	public void testEnded(String host) {
		martinisRef.set(null);
	}
}
