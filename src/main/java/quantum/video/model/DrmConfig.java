package quantum.video.model;

import java.util.List;

public record DrmConfig(String type, String licenseUrl, List<DrmKey> keys) {
}
