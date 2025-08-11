package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Analytics record for tracking and configuration of analytics data.
 * This record holds configuration parameters for the analytics service,
 * including maximum events, timeouts, and service identification.
 *
 * @param maxEvents Maximum number of events to process
 * @param timeout Timeout duration in milliseconds
 * @param key API key for analytics service
 * @param serviceProviderId Identifier for the service provider
 * @param version Version string for analytics compatibility
 * @param randomizationWindow Window size for randomization calculations
 * @param host Host address for the analytics service
 * @param port Port number for the analytics service
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record Analytics(
        int maxEvents,
        int timeout,
        String key,
        String serviceProviderId,
        String version,
        int randomizationWindow,
        String host,
        String port
) {
}
