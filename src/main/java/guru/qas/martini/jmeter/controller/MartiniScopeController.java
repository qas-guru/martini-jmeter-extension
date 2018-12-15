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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterThread;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jorphan.collections.SearchByClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import gherkin.ast.Feature;
import gherkin.ast.Location;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Tag;
import guru.qas.martini.Martini;
import guru.qas.martini.SyntheticMartini;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.gherkin.FeatureWrapper;
import guru.qas.martini.gherkin.Recipe;
import guru.qas.martini.jmeter.Variables;
import guru.qas.martini.result.DefaultMartiniResult;
import guru.qas.martini.result.MartiniResult;
import guru.qas.martini.tag.Categories;

@SuppressWarnings("WeakerAccess")
@Configurable
public class MartiniScopeController extends AbstractGenericController implements LoopIterationListener {

	// Shared.
	protected MartiniScopeControllerBean delegate;
	protected Categories categories;
	protected SuiteIdentifier suiteIdentifier;

	// Per-thread.
	protected MartiniResult martiniResult;

	@Autowired
	void set(MartiniScopeControllerBean b) {
		this.delegate = b;
	}

	@Autowired
	void set(Categories c) {
		this.categories = c;
	}

	@Autowired
	void set(SuiteIdentifier i) {
		this.suiteIdentifier = i;
	}

	public MartiniScopeController() {
		super();
	}

	@Override
	public Object clone() {
		Object o = super.clone();
		MartiniScopeController clone = MartiniScopeController.class.cast(o);
		clone.delegate = delegate;
		clone.categories = categories;
		clone.suiteIdentifier = suiteIdentifier;
		return clone;
	}

	@Override
	protected BeanInfoSupport getBeanInfoSupport() {
		return new MartiniScopeControllerBeanInfo();
	}

	@Override
	protected void completeSetup() {
		ConfigurableApplicationContext springContext = Variables.getSpringApplicationContext();
		AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
		beanFactory.autowireBean(this);
	}

	@Override
	public void iterationStart(LoopIterationEvent event) {
		endScenario();
	}

	protected void endScenario() {
		delegate.publishAfterScenario(martiniResult);
		martiniResult = null;
	}

	@Override
	public Sampler next() {
		if (null == martiniResult) {
			beginScenario();
		}
		return super.next();
	}

	protected void beginScenario() {
		Martini martini = Variables.getMartini().orElseGet(this::getSyntheticMartini);
		MartiniResult result = DefaultMartiniResult.builder()
			.setMartiniSuiteIdentifier(suiteIdentifier)
			.setMartini(martini)
			.build(categories);
		delegate.publishBeforeScenario(result);
		this.martiniResult = result;
	}

	protected Martini getSyntheticMartini() {
		Recipe recipe = getRecipe();
		return new SyntheticMartini(recipe);
	}

	protected Recipe getRecipe() {
		FeatureWrapper featureWrapper = getFeatureWrapper();
		throw new UnsupportedOperationException();
		//return new DefaultRecipe(featureWrapper, pickle, pickleLocation, scenarioDefinition);
	}

	protected FeatureWrapper getFeatureWrapper() {
		Feature feature = getFeature();
		// TODO: resource is the Test Plan
		throw new UnsupportedOperationException();
		//return new FeatureWrapper(feature, resource);
	}

	protected Feature getFeature() {
		JMeterContext threadContext = super.getThreadContext();

		JMeterThread thread = threadContext.getThread();
		ListedHashTree tree = thread.getTestTree();
		SearchByClass<MartiniScopeController> controllerSearch = new SearchByClass<>(MartiniScopeController.class);
		tree.traverse(controllerSearch);

		HashTree subTree = controllerSearch.getSubTree(this);
		SearchByClass<Sampler> samplerSearch = new SearchByClass<>(Sampler.class);
		subTree.traverse(samplerSearch);

		Collection<Sampler> samplers = samplerSearch.getSearchResults();
		List<Sampler> enabledSamplers = samplers.stream()
			.filter(TestElement::isEnabled)
			.collect(Collectors.toList());

		List<Tag> tags = ImmutableList.of();
		Location location = new Location(0, 0);
		String language = JMeterUtils.getLocale().getLanguage();
		String keyword = "SyntheticFeature";
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		String name = threadGroup.getName();
		String description = threadGroup.getComment();
		List<ScenarioDefinition> definitions = ImmutableList.of();

		return new Feature(tags, location, language, keyword, name, description, definitions);
	}

	@Override
	protected void beginTearDown() {
		if (null != delegate) {
			endScenario();
			delegate = null;
		}
		martiniResult = null;
		categories = null;
		suiteIdentifier = null;
	}
}