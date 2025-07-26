package quantum.video.api;

/**
 * Data Transfer Object for a DRM (Digital Rights Management) key.
 * <p>
 * Represents a single key used for decrypting protected content.
 * </p>
 *
 * @param kid Key identifier (KID)
 * @param key The actual decryption key (base64 or hex encoded)
 */
public record DrmKey(String kid, String key) {
}