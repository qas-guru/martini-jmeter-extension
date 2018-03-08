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
import org.apache.jmeter.testelement.property.JMeterProperty;
import org.apache.jmeter.testelement.property.ObjectProperty;
import org.apache.jmeter.threads.JMeterContext;
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
			JMeterProperty property = new ObjectProperty(PROPERTY_MARTINI_ITERATOR, i);
			super.setProperty(property);
			super.setTemporary(property);
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
			throw new MartiniException(
				"Spring context not set; is there a Martini PreProcessor configured at the Test Plan level?");
		}
		return getNewIterator(ConfigurableApplicationContext.class.cast(o));
	}

	protected Iterator<Martini> getNewIterator(ConfigurableApplicationContext context) {
		Mixologist mixologist = context.getBean(Mixologist.class);
		String spelFilter = getSpelFilter();
		Collection<Martini> martinis = spelFilter.isEmpty() ? mixologist.getMartinis() : mixologist.getMartinis(spelFilter);
		return getNewIterator(martinis);
	}

	protected Iterator<Martini> getNewIterator(Collection<Martini> martinis) {
		if (martinis.isEmpty()) {
			String spelFilter = getSpelFilter();
			String message = spelFilter.isEmpty() ?
				"No Martinis found." : String.format("No Martinis found matching filter '%s'.", spelFilter);
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
		Martini martini = getCurrentMartini().orElseGet(() -> {
			Iterator<Martini> i = getIterator().orElse(null);
			Martini next = null != i && i.hasNext() ? i.next() : null;
			setCurrent(next);
			return next;
		});

		Sampler sampler = null;
		if (null == martini) {
			setDone(true);
		}
		else {
			sampler = super.next();
			if (null == sampler) {
				super.reInitialize();
				sampler = super.next();
			}
		}

		updateSamplerContext(martini, sampler);
		return sampler;
	}

	private void updateSamplerContext(Martini martini, Sampler sampler) {
		if (null != sampler) {
			JMeterContext threadContext = sampler.getThreadContext();
			Map<String, Object> samplerContext = threadContext.getSamplerContext();
			if (null == martini) {
				samplerContext.remove(PROPERTY_CURRENT_MARTINI);
			}
			else {
				samplerContext.put(PROPERTY_CURRENT_MARTINI, martini);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<Iterator<Martini>> getIterator() {
		JMeterProperty property = super.getProperty(PROPERTY_MARTINI_ITERATOR);
		Object o = property.getObjectValue();
		Iterator<Martini> i = Iterator.class.isInstance(o) ? (Iterator<Martini>) o : null;
		return Optional.ofNullable(i);
	}

	private Optional<Martini> getCurrentMartini() {
		JMeterProperty property = super.getProperty(PROPERTY_CURRENT_MARTINI);
		Object o = property.getObjectValue();
		Martini martini = Martini.class.isInstance(o) ? Martini.class.cast(o) : null;
		return Optional.ofNullable(martini);
	}

	private void setCurrent(Martini martini) {
		JMeterProperty property = new ObjectProperty(PROPERTY_CURRENT_MARTINI, martini);
		super.setProperty(property);
		super.setTemporary(property);
	}
}
