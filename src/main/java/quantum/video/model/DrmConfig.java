package quantum.video.model;

import java.util.List;

/**
 * Configuration record for Digital Rights Management (DRM) settings.
 * <p>
 * This record encapsulates the DRM configuration for protected content in the streaming system.
 * It contains information about the DRM type (e.g., Widevine, PlayReady, FairPlay),
 * the license server URL, and a list of encryption keys used for content protection.
 * </p>
 * <p>
 * DRM configurations are typically associated with channels or programs that require
 * content protection and are referenced in streaming manifests to ensure proper
 * content encryption and decryption.
 * </p>
 *
 * @param type The DRM system type (e.g., "widevine", "playready", "fairplay")
 * @param licenseUrl The URL of the license server for acquiring DRM licenses
 * @param keys A list of {@link DrmKey} objects containing encryption key information
 */
public record DrmConfig(String type, String licenseUrl, List<DrmKey> keys) {
}
