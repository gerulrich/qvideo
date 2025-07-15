package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * HostValue record that represents a host address or endpoint configuration.
 * This record encapsulates a host address used for service connections.
 *
 * @param host Host address or endpoint URL
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record HostValue(
        String host
) {
}
