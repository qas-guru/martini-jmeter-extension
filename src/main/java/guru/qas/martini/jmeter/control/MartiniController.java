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
import java.util.Map;
import java.util.Optional;

import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import guru.qas.martini.Martini;
import guru.qas.martini.MartiniException;
import guru.qas.martini.Mixologist;

import static guru.qas.martini.jmeter.Constants.KEY_SPRING_CONTEXT;

@SuppressWarnings("WeakerAccess")
public class MartiniController extends GenericController implements LoopIterationListener {

	private static final long serialVersionUID = 2700570246170278883L;

	private static final Logger LOGGER = LoggerFactory.getLogger(MartiniController.class);
	protected static final String PROPERTY_SPEL_FILTER = "martini.spel.filter";
	protected static final String PROPERTY_MARTINI_ITERATOR = "martini.iterator";
	protected static final String PROPERTY_CURRENT_MARTINI = "martini.current";

	public void setSpelFilter(String spelFilter) {
		String normalized = null == spelFilter ? "" : spelFilter.replaceAll("\\s+", " ").trim();
		super.setProperty(PROPERTY_SPEL_FILTER, normalized);
	}

	public String getSpelFilter() {
		return super.getPropertyAsString(PROPERTY_SPEL_FILTER);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void iterationStart(LoopIterationEvent event) {
		try {
			Iterator<Martini> i = getNewIterator();
			JMeterContext threadContext = super.getThreadContext();
			JMeterVariables variables = threadContext.getVariables();
			variables.putObject(PROPERTY_MARTINI_ITERATOR, i);
		}
		catch (MartiniException e) {
			JMeterUtils.reportErrorToUser(e.getMessage(), "Martini Error", e);
		}
		catch (Exception e) {
			LOGGER.error("unable to retrieve test scenarios", e);
			JMeterUtils.reportErrorToUser("Unable to retrieve test scenarios.", "Martini Error", e);
		}
	}

	protected Iterator<Martini> getNewIterator() {
		JMeterContext threadContext = super.getThreadContext();
		Map<String, Object> samplerContext = threadContext.getSamplerContext();
		Object o = samplerContext.getOrDefault(KEY_SPRING_CONTEXT, null);
		if (null == o) {
			String message = String.format("Spring context not set; %s samplers will not be executed.", getName());
			throw new MartiniException(message);
		}
		return getNewIterator(ConfigurableApplicationContext.class.cast(o));
	}

	protected Iterator<Martini> getNewIterator(ConfigurableApplicationContext context) {
		Mixologist mixologist = context.getBean(Mixologist.class);
		String spelFilter = getSpelFilter();
		Collection<Martini> martinis = spelFilter.isEmpty() ?
			mixologist.getMartinis() : mixologist.getMartinis(spelFilter);
		return getNewIterator(martinis);
	}

	protected Iterator<Martini> getNewIterator(Collection<Martini> martinis) {
		if (martinis.isEmpty()) {
			String action = String.format("%s samplers will not be executed", getName());
			String spelFilter = getSpelFilter();
			String message = spelFilter.isEmpty() ?
				String.format("No Martini found; %s.", action) :
				String.format("No Martini found matching filter %s; %s.", spelFilter, action);
			throw new MartiniException(message);
		}
		return martinis.iterator();
	}

	@Override
	public void triggerEndOfLoop() {
		super.triggerEndOfLoop();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Sampler next() {
		Sampler sampler = super.next();
		Martini martini = null == sampler ? getNextMartini() : getCurrentMartini();

		if (null == martini) {
			sampler = null;
		}
		else if (null == sampler) {
			super.reInitialize(); // TODO: reset iterator?
			sampler = super.next();
		}

		if (null == sampler) {
			setDone(true);
		}
		else {
			JMeterContext threadContext = sampler.getThreadContext();
			Map<String, Object> samplerContext = threadContext.getSamplerContext();
			samplerContext.put(PROPERTY_CURRENT_MARTINI, martini);
		}
		return sampler;
	}

	private Martini getCurrentMartini() {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(PROPERTY_CURRENT_MARTINI);
		Martini martini = Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
		return null == martini ? getNextMartini() : martini;
	}

	private Martini getNextMartini() {
		Iterator<Martini> i = this.getIterator().orElse(null);
		Martini next = null != i && i.hasNext() ? i.next() : null;
		this.setCurrent(next);
		return next;
	}

	@SuppressWarnings("unchecked")
	private Optional<Iterator<Martini>> getIterator() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		Object o = variables.getObject(PROPERTY_MARTINI_ITERATOR);
		Iterator<Martini> i = Iterator.class.isInstance(o) ? (Iterator<Martini>) o : null;
		return Optional.ofNullable(i);
	}

	private void setCurrent(Martini martini) {
		JMeterContext threadContext = getThreadContext();
		JMeterVariables variables = threadContext.getVariables();
		if (null == martini) {
			variables.remove(PROPERTY_CURRENT_MARTINI);
		}
		else {
			variables.putObject(PROPERTY_CURRENT_MARTINI, martini);
		}
	}
}
