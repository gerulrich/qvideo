package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import quantum.video.model.DrmConfig;

import java.util.List;
import java.util.Optional;

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
                        .map(list -> list.stream()
                                .map(key -> new DrmKey(key.kid(), key.key()))
                                .toList())
                        .orElse(null)
        );
    }
}