package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * MamushkaRequest represents a token authentication request payload.
 * This record encapsulates all required parameters for token generation
 * and device authentication with the auth SDK service.
 *
 * @param jwt JWT token for authentication
 * @param token Session token received from the session service
 * @param deviceToken Device-specific token for identification
 * @param casId Conditional Access System identifier
 * @param deviceOs Operating system of the device
 * @param follow Follow configuration parameters
 * @param deviceInfo Detailed information about the device
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record MamushkaRequest(
        String jwt,
        String token,
        String deviceToken,
        String casId,
        String deviceOs,
        Follow follow,
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