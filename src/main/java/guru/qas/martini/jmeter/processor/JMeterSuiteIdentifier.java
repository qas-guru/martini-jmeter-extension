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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.util.JMeterUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import guru.qas.martini.event.SuiteIdentifier;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("WeakerAccess")
public class JMeterSuiteIdentifier implements SuiteIdentifier {

	private final UUID id;
	private final String name;
	private final long startTimestamp;
	private final String hostname;
	private final String hostAddress;
	private final String username;
	private final ImmutableList<String> profiles;
	private final ImmutableMap<String, String> environmentVariables;

	protected JMeterSuiteIdentifier(
		UUID id,
		String name,
		long startTimestamp,
		String hostname,
		String hostAddress,
		String username,
		ImmutableList<String> profiles,
		ImmutableMap<String, String> environmentVariables
	) {
		this.id = id;
		this.name = name;
		this.startTimestamp = startTimestamp;
		this.hostname = hostname;
		this.hostAddress = hostAddress;
		this.username = username;
		this.profiles = profiles;
		this.environmentVariables = environmentVariables;
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
	public String getHostname() {
		return hostname;
	}

	@Override
	public String getHostAddress() {
		return hostAddress;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public Collection<String> getProfiles() {
		return profiles;
	}

	@Override
	public Map<String, String> getEnvironmentVariables() {
		return environmentVariables;
	}

	static Builder builder() {
		return new Builder();
	}

	static class Builder {

		private JMeterContext jmeterContext;
		private ConfigurableApplicationContext springContext;

		private Builder() {
		}

		Builder setJMeterContext(JMeterContext c) {
			this.jmeterContext = c;
			return this;
		}

		Builder setConfigurableApplicationContext(ConfigurableApplicationContext c) {
			this.springContext = c;
			return this;
		}

		JMeterSuiteIdentifier build() {
			checkNotNull(jmeterContext, "null JMeterContext");
			checkNotNull(springContext, "null SpringContext");
			UUID id = UUID.randomUUID();

			String name = springContext.getDisplayName();

			long timestamp = springContext.getStartupDate();
			String hostname = JMeterUtils.getLocalHostName();
			String hostAddress = JMeterUtils.getLocalHostIP();
			String username = System.getProperty("user.name");

			ConfigurableEnvironment environment = springContext.getEnvironment();
			String[] profileArray = environment.getActiveProfiles();
			ImmutableList<String> profiles = ImmutableList.<String>builder().add(profileArray).build();

			Map<String, String> map = new HashMap<>();

			for (PropertySource<?> propertySource : environment.getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					EnumerablePropertySource enumerable = EnumerablePropertySource.class.cast(propertySource);
					for (String key : enumerable.getPropertyNames()) {
						Object value = propertySource.getProperty(key);
						map.put(key, null == value ? null : value.toString());
					}
				}
			}

			ImmutableMap<String, String> systemProperties = ImmutableMap.copyOf(map);
			return new JMeterSuiteIdentifier(id, name, timestamp, hostname, hostAddress, username, profiles, systemProperties);
		}
	}
}
