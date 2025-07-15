package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Primary record that represents primary authentication configuration.
 * This record holds the PIN value used for user authentication or parental controls.
 *
 * @param pin PIN value for authentication or parental controls
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record Primary(
        int pin
) {
}
