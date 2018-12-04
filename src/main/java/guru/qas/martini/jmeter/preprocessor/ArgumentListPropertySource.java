package guru.qas.martini.jmeter.preprocessor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.jmeter.config.Argument;
import org.springframework.core.env.MapPropertySource;

import com.google.common.collect.ArrayListMultimap;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public final class ArgumentListPropertySource extends MapPropertySource {

	@SuppressWarnings("WeakerAccess")
	protected ArgumentListPropertySource(String sourceName, Map<String, Object> index) {
		super(sourceName, index);
	}

	@Override
	public Object getProperty(@Nonnull String name) {
		return super.getProperty(name);
	}

	public static Builder builder() {
		return new Builder();
	}

	@SuppressWarnings("WeakerAccess")
	public static final class Builder {

		private String name;
		private List<Argument> arguments;

		protected Builder() {
			this.arguments = new ArrayList<>();
		}

		public Builder setName(String s) {
			this.name = null == s ? null : s.trim();
			return this;
		}

		public Builder setArguments(List<Argument> a) {
			this.arguments.clear();

			if (null != a) {
				arguments.addAll(a);
			}
			return this;
		}

		public ArgumentListPropertySource build() {
			checkNotNull(name, "null String");
			checkArgument(!name.isEmpty(), "empty String");

			ArrayListMultimap<String, Object> index = getIndex();
			Map<String, Object> source = getSource(index);
			return new ArgumentListPropertySource(name, source);
		}

		protected ArrayListMultimap<String, Object> getIndex() {
			ArrayListMultimap<String, Object> index = ArrayListMultimap.create();
			arguments.stream()
				.filter(Objects::nonNull)
				.filter(argument -> {
					String name = argument.getName();
					return null != name && !name.trim().isEmpty();
				})
				.forEachOrdered(argument -> index.put(argument.getName(), argument.getValue()));
			return index;
		}

		protected Map<String, Object> getSource(ArrayListMultimap<String, Object> index) {
			Map<String, Object> source = new LinkedHashMap<>();
			Set<String> keys = index.keySet();
			keys.forEach(key -> {
				List<Object> objects = index.get(key);
				switch (objects.size()) {
					case 0:
						source.put(key, null);
						break;
					case 1:
						Object value = objects.get(0);
						source.put(key, value);
						break;
					default:
						source.put(key, objects);
				}
			});
			return source;
		}
	}
}
