package quantum.video.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Configuration class that registers classes for reflection in native mode.
 * <p>
 * This registrar ensures that specific classes, particularly those needed for JSON serialization/deserialization,
 * are available for reflection when running as a native executable with GraalVM.
 * <p>
 * The SnakeCaseStrategy is registered to enable proper JSON property naming convention support,
 * which converts camelCase Java property names to snake_case JSON field names.
 *
 * @see RegisterForReflection
 * @see SnakeCaseStrategy
 */
@RegisterForReflection(targets = SnakeCaseStrategy.class)
public class ReflectionRegistrar {
}
