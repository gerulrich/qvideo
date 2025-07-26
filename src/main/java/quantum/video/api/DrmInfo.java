package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import quantum.video.model.DrmConfig;

import java.util.List;
import java.util.Optional;

/**
 * Data Transfer Object for Digital Rights Management (DRM) information.
 * <p>
 * Contains DRM type, license URL, and a list of DRM keys for playback protection.
 * </p>
 *
 * @param type       DRM system type (e.g., Widevine, PlayReady)
 * @param licenseUrl License server URL for key acquisition
 * @param keys       List of DRM keys associated with the content
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DrmInfo(
        String type,
        String licenseUrl,
        List<DrmKey> keys)
{
    public DrmInfo(DrmConfig config) {
        this(
            config.type(),
            config.licenseUrl(),
            Optional.ofNullable(config.keys())
                .map(list -> list.stream().map(key -> new DrmKey(key.kid(), key.key())).toList())
                .orElse(null)
        );
    }
}