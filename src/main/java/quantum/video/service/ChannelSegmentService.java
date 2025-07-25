package quantum.video.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import quantum.video.model.Channel;
import quantum.video.repository.ChannelRepository;

import java.util.function.Function;

/**
 * Service for handling media segment operations for live channel streaming.
 * <p>
 * This service extends {@link AbstractStreamService} to provide specific functionality
 * for retrieving and streaming audio and video segments for live channels. It works
 * in conjunction with {@link ChannelManifestService} which handles the manifest files
 * that reference these segments.
 * </p>
 * <p>
 * The service utilizes Quarkus caching to optimize channel data retrieval and
 * leverages Mutiny's reactive types for efficient non-blocking I/O operations
 * when streaming potentially large media segments.
 * </p>
 * <p>
 * This service supports the following operations:
 * <ul>
 *   <li>Streaming audio segments for live channels</li>
 *   <li>Streaming video segments for live channels</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class ChannelSegmentService extends AbstractStreamService {

    /**
     * Cache for channel information to avoid repeated database queries.
     */
    @Inject
    @CacheName("channel")
    Cache cache;

    /**
     * Repository for retrieving channel information.
     */
    @Inject
    ChannelRepository repository;

    /**
     * Streams an audio segment for a live channel.
     * <p>
     * This method retrieves and streams an audio segment by:
     * <ol>
     *   <li>Retrieving the channel information (with caching)</li>
     *   <li>Extracting the path between domain and file from the channel URL</li>
     *   <li>Formatting the complete audio segment URL</li>
     *   <li>Streaming the audio content using the reactive framework</li>
     * </ol>
     * </p>
     * <p>
     * The method leverages Mutiny's {@link Multi} for reactive streaming of the
     * audio content with proper back-pressure handling.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param channel The channel code identifier
     * @param file The specific audio segment file identifier
     * @return A {@link Multi} emitting the audio segment content as {@link Buffer} chunks
     */
    public Uni<String> getAudioSegment(String host, String token, String channel, String file) {
        return  getChannel(channel)
                .onItem().ifNull().failWith(() -> new NotFoundException("Channel segment not found"))
                .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
                .map(path -> formatAudioUrl(host, token, path, channel, file));
    }

    /**
     * Streams a video segment for a live channel.
     * <p>
     * This method retrieves and streams a video segment by:
     * <ol>
     *   <li>Retrieving the channel information (with caching)</li>
     *   <li>Extracting the path between domain and file from the channel URL</li>
     *   <li>Formatting the complete video segment URL</li>
     *   <li>Streaming the video content using the reactive framework</li>
     * </ol>
     * </p>
     * <p>
     * The method leverages Mutiny's {@link Multi} for reactive streaming of the
     * video content with proper back-pressure handling. The video segments typically
     * contain H.264 (AVC1) encoded content.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param channel The channel code identifier
     * @param file The specific video segment file identifier
     * @return A {@link Multi} emitting the video segment content as {@link Buffer} chunks
     */
    public Uni<String> getVideoSegment(String host, String token, String channel, String file) {
        return getChannel(channel)
                .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
                .map(path -> formatVideoUrl(host, token, path, channel, file));
    }

    /**
     * Retrieves channel information with caching support.
     * <p>
     * This private helper method retrieves channel information by its code,
     * using the Quarkus cache system to optimize repeated requests for the same channel.
     * If the channel is not in cache, it's retrieved from the repository and then cached.
     * </p>
     *
     * @param channel The channel code to retrieve
     * @return A {@link Uni} containing the {@link Channel} information, or an error if not found
     */
    private Uni<Channel> getChannel(String channel) {
        return cache.get(channel, k -> repository.findByCode(k)).flatMap(Function.identity());
    }
}
