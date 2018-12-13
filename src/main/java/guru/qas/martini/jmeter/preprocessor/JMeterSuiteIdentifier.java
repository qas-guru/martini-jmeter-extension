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

package guru.qas.martini.jmeter.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import guru.qas.martini.event.SuiteIdentifier;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
@Configurable
public class JMeterSuiteIdentifier implements SuiteIdentifier, InitializingBean, ApplicationContextAware {

	protected final UUID id;
	protected String name;
	protected Long startTimestamp;
	protected String hostname;
	protected String hostAddress;
	protected String username;
	protected ImmutableSet<String> profiles;
	protected Map<String, String> environmentVariables;

	protected ApplicationContext springContext;

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Long getStartTimestamp() {
		return startTimestamp;
	}

	@Override
	public Optional<String> getHostName() {
		return Optional.ofNullable(hostname);
	}

	@Override
	public Optional<String> getHostAddress() {
		return Optional.ofNullable(hostAddress);
	}

	@Override
	public Optional<String> getUsername() {
		return Optional.of(username);
	}

	@Override
	public Collection<String> getProfiles() {
		return profiles;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	protected JMeterSuiteIdentifier() {
		this.id = UUID.randomUUID();
	}

	@Override
	public void setApplicationContext(@Nonnull ApplicationContext c) {
		this.springContext = checkNotNull(c, "null ApplicationContext");
	}

	@Override
	public void afterPropertiesSet() {
		name = springContext.getDisplayName();
		startTimestamp = JMeterContextService.getTestStartTime();
		hostname = JMeterUtils.getLocalHostName();
		hostAddress = JMeterUtils.getLocalHostIP();
		username = System.getProperty("user.name");
		setUpProfiles();
		setUpEnvironmentVariables();
	}

	protected void setUpProfiles() {
		Environment environment = springContext.getEnvironment();
		String[] activeProfiles = environment.getActiveProfiles();
		ArrayList<String> profileList = Lists.newArrayList(activeProfiles);
		profiles = ImmutableSet.copyOf(profileList);
	}

	public void setUpEnvironmentVariables() {
		Environment environment = springContext.getEnvironment();
		Map<String, String> index = new LinkedHashMap<>();

		if (ConfigurableEnvironment.class.isInstance(environment)) {
			ConfigurableEnvironment configurable = ConfigurableEnvironment.class.cast(environment);
			configurable.getPropertySources().stream()
				.filter(EnumerablePropertySource.class::isInstance)
				.map(EnumerablePropertySource.class::cast)
				.forEach(s -> {
					String[] propertyNames = s.getPropertyNames();
					Arrays.stream(propertyNames).forEach(key -> {
						Object property = s.getProperty(key);
						index.put(key, null == property ? null : property.toString());
					});
				});
		}

		environmentVariables = ImmutableMap.copyOf(index);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		JMeterSuiteIdentifier that = (JMeterSuiteIdentifier) o;
		return Objects.equal(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}
}
