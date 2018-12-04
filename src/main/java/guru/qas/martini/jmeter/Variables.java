package guru.qas.martini.jmeter;

import java.util.Optional;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.springframework.context.support.ResourceBundleMessageSource;

import static com.google.common.base.Preconditions.*;

@SuppressWarnings("WeakerAccess")
public class Variables {

	protected static final ResourceBundleMessageSource MESSAGE_SOURCE = new ResourceBundleMessageSource();

	static {
		MESSAGE_SOURCE.setBasename("VariablesBundle");
	}

	public static void put(String key, Object o) {
		checkNotNull(key, "null String");
		JMeterVariables variables = getVariables();
		variables.putObject(key, o);
	}

	public static <T> T getRequired(String key, Class<T> implementation) {
		checkNotNull(key, "null String");
		checkNotNull(implementation, "null Class");
		return get(key, implementation).orElseThrow(() -> {
			String message = getMessage("missing.variable", key);
			return new IllegalStateException(message);
		});
	}

	protected static String getMessage(String code, Object... args) {
		return Messages.getMessage(MESSAGE_SOURCE, code, args);
	}

	public static <T> Optional<T> get(String key, Class<T> implementation) {
		checkNotNull(key, "null String");
		checkNotNull(implementation, "null Class");

		JMeterVariables variables = getVariables();
		Object o = variables.getObject(key);

		T instance = null;
		if (null != o) {
			checkState(implementation.isInstance(o),
				getMessage("invalid.variable", key, implementation, o.getClass()));
			instance = implementation.cast(o);
		}
		return Optional.ofNullable(instance);
	}

	@SuppressWarnings({"UnusedReturnValue", "unused"})
	public static Object remove(String key) {
		checkNotNull(key, "null String");
		JMeterVariables variables = getVariables();
		return variables.remove(key);
	}

	protected static JMeterVariables getVariables() {
		JMeterContext threadContext = JMeterContextService.getContext();
		return threadContext.getVariables();
	}
}