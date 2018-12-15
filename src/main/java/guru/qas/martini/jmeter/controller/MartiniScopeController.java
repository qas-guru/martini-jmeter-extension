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
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.samplers.Sampler;
import org.apache.jmeter.testbeans.BeanInfoSupport;
import org.apache.jmeter.testelement.TestElement;
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
import org.springframework.core.io.ByteArrayResource;

import com.google.common.collect.ImmutableList;

import gherkin.ast.Feature;
import gherkin.ast.Location;
import gherkin.ast.Scenario;
import gherkin.ast.ScenarioDefinition;
import gherkin.ast.Step;
import gherkin.ast.Tag;
import gherkin.pickles.Pickle;
import gherkin.pickles.PickleLocation;
import gherkin.pickles.PickleStep;
import guru.qas.martini.Martini;
import guru.qas.martini.SyntheticMartini;
import guru.qas.martini.annotation.When;
import guru.qas.martini.event.SuiteIdentifier;
import guru.qas.martini.gherkin.DefaultRecipe;
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
		HashTree hashTree = getHashTree();
		Scenario scenario = getScenario(hashTree);
		FeatureWrapper featureWrapper = getFeatureWrapper(scenario, hashTree);

		Pickle pickle = getPickle(scenario, featureWrapper);
		PickleLocation location = pickle.getLocations().get(0);

		return new DefaultRecipe(featureWrapper, pickle, location, scenario);
	}

	protected HashTree getHashTree() {
		JMeterContext threadContext = super.getThreadContext();
		JMeterThread thread = threadContext.getThread();
		ListedHashTree tree = thread.getTestTree();
		SearchByClass<MartiniScopeController> controllerSearch = new SearchByClass<>(MartiniScopeController.class);
		tree.traverse(controllerSearch);
		return controllerSearch.getSubTree(this);
	}

	protected Scenario getScenario(HashTree hashTree) {
		Collection<Sampler> samplers = getSamplers(hashTree);
		List<Step> steps = samplers.stream()
			.filter(TestElement::isEnabled)
			.map(this::getStep)
			.collect(Collectors.toList());
		return getScenario(steps);
	}

	protected List<Sampler> getSamplers(HashTree hashTree) {
		SearchByClass<Sampler> samplerSearch = new SearchByClass<>(Sampler.class);
		hashTree.traverse(samplerSearch);

		Collection<Sampler> samplers = samplerSearch.getSearchResults();
		return samplers.stream()
			.filter(TestElement::isEnabled)
			.collect(Collectors.toList());
	}

	protected Step getStep(Sampler sampler) {
		Location location = new Location(0, 0);
		String keyword = When.class.getSimpleName();
		String text = sampler.getName();
		return new Step(location, keyword, text, null);
	}

	protected Scenario getScenario(List<Step> steps) {
		ImmutableList<Tag> tags = ImmutableList.of();
		Location location = new Location(0, 0);
		String keyword = Scenario.class.getSimpleName();
		String name = getName();
		String description = super.getComment();
		return new Scenario(tags, location, keyword, name, description, steps);
	}

	protected FeatureWrapper getFeatureWrapper(Scenario scenario, HashTree hashTree) {
		Feature feature = getFeature(scenario);
		String serialized = hashTree.toString();
		ByteArrayResource resource = new ByteArrayResource(serialized.getBytes(), "JMeter HashTree");
		return new FeatureWrapper(feature, resource);
	}

	protected Feature getFeature(Scenario scenario) {
		List<Tag> tags = ImmutableList.of();
		Location location = new Location(0, 0);
		String language = JMeterUtils.getLocale().getLanguage();
		String keyword = Feature.class.getSimpleName();

		JMeterContext threadContext = super.getThreadContext();
		AbstractThreadGroup threadGroup = threadContext.getThreadGroup();
		String name = threadGroup.getName();
		String description = threadGroup.getComment();

		List<ScenarioDefinition> definitions = ImmutableList.of(scenario);
		return new Feature(tags, location, language, keyword, name, description, definitions);
	}

	protected Pickle getPickle(Scenario scenario, FeatureWrapper featureWrapper) {
		PickleLocation location = new PickleLocation(0, 0);

		String name = featureWrapper.getName();
		Locale locale = JMeterUtils.getLocale();
		String language = locale.getLanguage();

		List<PickleStep> pickleSteps = scenario.getSteps().stream()
			.map(step -> new PickleStep(step.getText(), ImmutableList.of(), ImmutableList.of(location)))
			.collect(Collectors.toList());

		return new Pickle(name, language, pickleSteps, ImmutableList.of(), ImmutableList.of(location));
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