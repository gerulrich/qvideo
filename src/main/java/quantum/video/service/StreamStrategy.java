package quantum.video.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClientRequest;

/**
 * Strategy interface defining methods for video content streaming operations.
 * <p>
 * This interface abstracts streaming functionality to allow different implementations
 * based on content type, protocol, or platform-specific requirements. It provides
 * minimal methods to facilitate testing through mocking.
 * </p>
 */
public interface StreamStrategy {

    /**
     * Gets an HTTP request for the specified URL with appropriate headers.
     *
     * @param url The target URL to make the request to
     * @return A {@link Uni} containing the prepared {@link HttpClientRequest}
     */
    Uni<HttpClientRequest> get(String url);

    /**
     * Streams content from the specified URL.
     * <p>
     * This method handles the entire streaming process including connection
     * establishment, request sending, and response processing. It returns
     * content as a stream of {@link Buffer} objects.
     * </p>
     *
     * @param url The URL to stream content from
     * @return A {@link Multi} emitting {@link Buffer} chunks containing the streamed content
     */
    Multi<Buffer> stream(String url);
}
