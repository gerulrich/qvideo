package quantum.video.builder;

import io.vertx.mutiny.core.MultiMap;

public class HeadersBuilder {
    private final MultiMap headers;

    public HeadersBuilder() {
        this.headers = MultiMap.caseInsensitiveMultiMap();
    }

    public HeadersBuilder add(String key, String value) {
        headers.add(key, value);
        return this;
    }

    public MultiMap build() {
        return headers;
    }
}
