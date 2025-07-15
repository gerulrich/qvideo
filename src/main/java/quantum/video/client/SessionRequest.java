package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * SessionRequest record represents a request to create or validate a user session.
 * Contains device information and authorization token needed for session management.
 *
 * @param deviceInfo Detailed information about the client device
 * @param deviceToken Token used for device authentication and identification
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record SessionRequest(DeviceInfo deviceInfo, String deviceToken) {
}
