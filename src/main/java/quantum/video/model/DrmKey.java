package quantum.video.model;

/**
 * Represents a Digital Rights Management (DRM) encryption key.
 * <p>
 * This record encapsulates the key identifier (KID) and the actual encryption key
 * used for content protection in a DRM system. These keys are essential components
 * of the content protection workflow and are used in the encryption and decryption
 * processes for protected media.
 * </p>
 * <p>
 * DrmKey objects are typically part of a {@link DrmConfig} and are used during manifest
 * generation to include proper encryption information in streaming manifests like DASH MPD
 * or HLS playlists.
 * </p>
 *
 * @param kid The key identifier, typically a UUID used to identify this specific encryption key
 * @param key The encryption key value, usually in a base64 or hex encoded format
 */
public record DrmKey(String kid, String key) {

}