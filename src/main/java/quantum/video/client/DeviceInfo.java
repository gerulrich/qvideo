package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * DeviceInfo record that contains all the device-specific information.
 * This record holds various properties related to the device's hardware,
 * software, and network configuration used for authentication and analytics.
 *
 * @param networkType Type of network connection (e.g., "Broadband")
 * @param playerType Type of media player being used
 * @param deviceOsVersion Version of the device operating system
 * @param casId Conditional Access System identifier
 * @param deviceModel Model name/number of the device
 * @param mac MAC address of the device
 * @param deviceName User-friendly name of the device
 * @param appVersion Version of the application running on the device
 * @param deviceType Category of device (e.g., "stationary", "mobile")
 * @param deviceBrand Brand/manufacturer of the device
 * @param deviceOs Operating system of the device
 * @param firmwareVersion Version of the device firmware
 * @param uuid Universally unique identifier for the device
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record DeviceInfo(
    String networkType,
    String playerType,
    String deviceOsVersion,
    String casId,
    String deviceModel,
    String mac,
    String deviceName,
    String appVersion,
    String deviceType,
    String deviceBrand,
    String deviceOs,
    String firmwareVersion,
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
