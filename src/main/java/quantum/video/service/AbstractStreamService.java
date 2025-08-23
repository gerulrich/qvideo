package quantum.video.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClientRequest;
import jakarta.inject.Inject;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

/**
 * Abstract base service for streaming functionality.
 * <p>
 * This service provides common functionality for stream-related operations,
 * serving as a base class for specialized streaming services like
 * {@link ChannelManifestService} and {@link ProgramManifestService}.
 * It handles HTTP client setup, URL formatting, and content streaming.
 * </p>
 * <p>
 * The service uses a reactive approach with Mutiny and Vert.x for non-blocking I/O
 * operations, which is essential for efficient handling of potentially large
 * media streams.
 * </p>
 * <p>
 * Key functionalities include:
 * <ul>
 *   <li>Streaming strategy management for HTTP interactions</li>
 *   <li>Content proxying with retry capability</li>
 *   <li>URL formatting for different streaming content types</li>
 *   <li>Base64 encoding/decoding for security-related operations</li>
 * </ul>
 * </p>
 */
public abstract class AbstractStreamService {

    @Inject
    protected StreamStrategy stream;

    /**
     * Base64 encoder for encoding values in URLs.
     */
    protected Base64.Encoder base64Encoder = Base64.getEncoder();

    /**
     * Base64 decoder for decoding host values from URLs.
     */
    protected Base64.Decoder base64Decoder = Base64.getDecoder();

    /**
     * Gets an HTTP request with standardized headers for streaming operations.
     * <p>
     * The request includes common headers like User-Agent and origin identifiers
     * to ensure consistent behavior when accessing streaming content.
     * </p>
     *
     * @param url The target URL to make the request to
     * @return A {@link Uni} containing the prepared {@link HttpClientRequest}
     */
    protected Uni<HttpClientRequest> get(String url) {
        return stream.get(url);
    }

    /**
     * Proxies a file from a given URL.
     * <p>
     * This method implements a reactive streaming approach to efficiently proxy content
     * from an external source. It uses a retry mechanism for resilience and proper
     * error handling.
     * </p>
     * <p>
     * The content is streamed in chunks using Mutiny's Multi, enabling non-blocking
     * transfer of potentially large files while managing back-pressure.
     * </p>
     *
     * @param url The URL of the file to proxy
     * @return A {@link Multi} emitting the file's content as {@link Buffer} chunks
     */
    public Multi<Buffer> stream(String url) {
        return stream.stream(url);
    }

    /**
     * Formats a URL for audio segment content.
     * <p>
     * Constructs a fully qualified URL pointing to an audio segment file,
     * using the standard pattern for audio content in the streaming system.
     * The host value is decoded from Base64 for security.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param path The path component between the token and channel
     * @param channel The channel identifier
     * @param file The specific audio segment file identifier
     * @return A formatted URL string for the audio segment
     */
    protected String formatAudioUrl(String host, String token, String path, String channel, String file) {
        return  "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + "-mp4a_" + file + ".mp4";
    }

    /**
     * Formats a URL for video segment content.
     * <p>
     * Constructs a fully qualified URL pointing to a video segment file,
     * using the standard pattern for video content in the streaming system.
     * The host value is decoded from Base64 for security.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param path The path component between the token and channel
     * @param channel The channel identifier
     * @param file The specific video segment file identifier
     * @return A formatted URL string for the video segment
     */
    protected String formatVideoUrl(String host, String token, String path, String channel, String file) {
        return  "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + "-avc1_" + file + ".mp4";
    }

    /**
     * Formats a URL for MPD (Media Presentation Description) manifest file.
     * <p>
     * Constructs a fully qualified URL pointing to a DASH manifest file,
     * using the standard pattern for MPD content in the streaming system.
     * The host value is decoded from Base64 for security.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param channel The channel identifier
     * @param path The path component between the token and channel
     * @return A formatted URL string for the MPD manifest
     */
    protected String formatMpdUrl(String host, String token, String channel, String path) {
        return "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + ".mpd";
    }

    /**
     * Extracts the base path from a URL string, up to and including the last slash.
     * <p>
     * For example, given "<a href="https://domain.com/path/to/file.mpd">...</a>", returns "/path/to/".
     * This is useful for reconstructing resource URLs for streaming segments.
     * </p>
     *
     * @param urlString the full URL string to parse
     * @return the base path segment of the URL, including the trailing slash
     * @throws IllegalArgumentException if the input is not a valid URL
     */
    protected String getBasePath(String urlString) {
        return getUrl(urlString).map(url -> {
            String path = url.getPath();
            int lastSlash = path.lastIndexOf("/");
            return path.substring(0, lastSlash + 1);
        }).orElseThrow(() -> new IllegalArgumentException("Invalid URL: " + urlString));
    }

    /**
     * Attempts to convert a string representation of a URL to a {@link URL} object.
     * <p>
     * This method parses the provided string as a URI and then converts it to a URL.
     * If the input is not a valid URI or URL, it returns {@link Optional#empty()}.
     * </p>
     *
     * @param url the string representation of the URL
     * @return an {@link Optional} containing the {@link URL} if valid, or empty if invalid
     */
    private Optional<URL> getUrl(String url) {
        try {
            return Optional.of(new URI(url).toURL());
        } catch (URISyntaxException | MalformedURLException e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the channel code from a URL.
     * <p>
     * Takes a URL like "<a href="https://qvideo.com/sample/CH1.mpd">...</a>"
     * and returns the filename without extension (e.g., "CH1").
     * </p>
     *
     * @param url The full URL containing the channel code
     * @return The extracted channel code
     * @throws IllegalArgumentException if the URL format is invalid
     */
    protected String getChannelCodeFromUrl(String url) {
        return Optional.ofNullable(url)
            .map(u -> u.substring(u.lastIndexOf('/') + 1))
            .filter(fileName -> !fileName.isEmpty())
            .map(fileName -> {
                int dotIndex = fileName.indexOf('.');
                return dotIndex == -1 ? fileName : fileName.substring(0, dotIndex);
            })
            .orElseThrow(() -> new IllegalArgumentException("Invalid URL format: " + url));
    }
}
