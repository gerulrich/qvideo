package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DeviceInfo record that contains all the device-specific information.
 * This record holds various properties related to the device's hardware,
 * software, and network configuration used for authentication and analytics.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record DeviceInfo(
    /**
     * Type of network connection (e.g., "Broadband")
     */
    String networkType,

    /**
     * Type of media player being used
     */
    String playerType,

    /**
     * Version of the device operating system
     */
    String deviceOsVersion,

    /**
     * Conditional Access System identifier
     */
    String casId,

    /**
     * Model name/number of the device
     */
    String deviceModel,

    /**
     * MAC address of the device
     */
    String mac,

    /**
     * User-friendly name of the device
     */
    String deviceName,

    /**
     * Version of the application running on the device
     */
    String appVersion,

    /**
     * Category of device (e.g., "stationary", "mobile")
     */
    String deviceType,

    /**
     * Brand/manufacturer of the device
     */
    String deviceBrand,

    /**
     * Operating system of the device
     */
    String deviceOs,

    /**
     * Version of the device firmware
     */
    String firmwareVersion,

    /**
     * Universally unique identifier for the device
     */
    String uuid
) {
    /**
     * Convenience constructor that provides default values for certain fields.
     *
     * @param deviceOsVersion Version of the device operating system
     * @param casId Conditional Access System identifier
     * @param mac MAC address of the device
     * @param appVersion Version of the application
     * @param uuid Universally unique identifier for the device
     */
    public DeviceInfo(
        String deviceOsVersion,
        String casId,
        String mac,
        String appVersion,
        String uuid)
    {
        this(
            "Broadband",
            "Entone",
            deviceOsVersion,
            casId,
            "AndroidHomeLab",
            mac,
            "Homelab device",
            appVersion,
            "stationary",
            "Google",
            "AndroidTV",
            "",
            uuid);
    }
}
