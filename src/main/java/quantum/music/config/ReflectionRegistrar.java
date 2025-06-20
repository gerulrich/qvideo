package quantum.music.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(targets = SnakeCaseStrategy.class)
public class ReflectionRegistrar {
}
