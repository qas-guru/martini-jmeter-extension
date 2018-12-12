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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import guru.qas.martini.event.SuiteIdentifier;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class JMeterSuiteIdentifier implements SuiteIdentifier {

	protected final UUID id;
	protected String name;
	protected final long startTimestamp;
	protected final String hostname;
	protected final String hostAddress;
	protected final String username;
	protected final ImmutableSet<String> profiles;
	protected final ImmutableMap<String, String> environmentVariables;

	protected void setName(String s) {
		this.name = s;
	}

	protected JMeterSuiteIdentifier(
		@Nonnull UUID id,
		@Nonnull String name,
		long startTimestamp,
		String hostname,
		String hostAddress,
		String username,
		Iterable<String> profiles,
		Map<String, String> environmentVariables
	) {
		this.id = checkNotNull(id, "null UUID");
		this.name = checkNotNull(name, "null String name");
		this.startTimestamp = startTimestamp;
		this.hostname = hostname;
		this.hostAddress = hostAddress;
		this.username = username;
		this.profiles = null == profiles ? ImmutableSet.of() : ImmutableSet.copyOf(Sets.newLinkedHashSet(profiles));
		this.environmentVariables = null == environmentVariables ? ImmutableMap.of() : ImmutableMap.copyOf(environmentVariables);
	}

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
		return Optional.ofNullable(username);
	}

	@Override
	public Collection<String> getProfiles() {
		return profiles;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	public static JMeterSuiteIdentifier getInstance(@Nonnull ClassPathXmlApplicationContext springContext) {
		checkNotNull(springContext, "null ApplicationContext");

		UUID uuid = UUID.randomUUID();
		String name = getSuiteName(springContext);
		long startTime = JMeterContextService.getTestStartTime();
		String hostname = JMeterUtils.getLocalHostName();
		String hostAddress = JMeterUtils.getLocalHostIP();
		String username = System.getProperty("user.name");
		Collection<String> profiles = getProfiles(springContext);
		Map<String, String> environmentVariables = getEnvironmentVariables(springContext);
		return new JMeterSuiteIdentifier(uuid, name, startTime, hostname, hostAddress, username, profiles, environmentVariables);
	}

	protected static String getSuiteName(ClassPathXmlApplicationContext springContext) {
		return springContext.getDisplayName();
	}

	protected static Collection<String> getProfiles(ApplicationContext springContext) {
		Environment environment = springContext.getEnvironment();
		String[] activeProfiles = environment.getActiveProfiles();
		return Sets.newLinkedHashSet(Lists.newArrayList(activeProfiles));
	}

	protected static Map<String, String> getEnvironmentVariables(ClassPathXmlApplicationContext springContext) {
		ConfigurableEnvironment environment = springContext.getEnvironment();
		Map<String, String> index = new LinkedHashMap<>();
		environment.getPropertySources().stream()
			.filter(EnumerablePropertySource.class::isInstance)
			.map(EnumerablePropertySource.class::cast)
			.forEach(s -> {
				String[] propertyNames = s.getPropertyNames();
				Arrays.stream(propertyNames).forEach(key -> {
					Object property = s.getProperty(key);
					index.put(key, null == property ? null : property.toString());
				});
			});
		return index;
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
