package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * MamushkaRequest represents a token authentication request payload.
 * This record encapsulates all required parameters for token generation
 * and device authentication with the auth SDK service.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record MamushkaRequest(
        /**
         * JWT token for authentication
         */
        String jwt,

        /**
         * Session token received from the session service
         */
        String token,

        /**
         * Device-specific token for identification
         */
        String deviceToken,

        /**
         * Conditional Access System identifier
         */
        String casId,

        /**
         * Operating system of the device
         */
        String deviceOs,

        /**
         * Follow configuration parameters
         */
        Follow follow,

        /**
         * Detailed information about the device
         */
        DeviceInfo deviceInfo
) {
    /**
     * Convenience constructor with default values for certain fields.
     * Sets jwt to an empty string, deviceOs to "AndroidTV", and creates a new default Follow instance.
     *
     * @param token Session token from the session service
     * @param deviceToken Device-specific token for identification
     * @param casId Conditional Access System identifier
     * @param deviceInfo Detailed information about the device
     */
    public MamushkaRequest(
            String token,
            String deviceToken,
            String casId,
            DeviceInfo deviceInfo
    ) {
        this(
            "",
            token,
            deviceToken,
            casId,
            "AndroidTV",
            new Follow(),
            deviceInfo
        );
    }
}