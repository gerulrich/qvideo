package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Analytics record for tracking and configuration of analytics data.
 * This record holds configuration parameters for the analytics service,
 * including maximum events, timeouts, and service identification.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record Analytics(
        /**
         * Maximum number of events to process
         */
        int maxEvents,

        /**
         * Timeout duration in milliseconds
         */
        int timeout,

        /**
         * API key for analytics service
         */
        String key,

        /**
         * Identifier for the service provider
         */
        String serviceProviderId,

        /**
         * Version string for analytics compatibility
         */
        String version,

        /**
         * Window size for randomization calculations
         */
        int randomizationWindow,

        /**
         * Host address for the analytics service
         */
        String host,

        /**
         * Port number for the analytics service
         */
        String port
) {
}
