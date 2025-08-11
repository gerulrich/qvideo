package quantum.video.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import org.jboss.logging.Logger;
import quantum.video.model.Channel;
import quantum.video.repository.ChannelRepository;

import java.util.function.Function;

/**
 * Service for handling channel manifest operations for live streaming.
 * <p>
 * This service extends {@link AbstractStreamService} to provide specific functionality
 * for channel manifest operations. It manages the retrieval and generation of DASH
 * manifests for live streaming channels, with caching support for improved performance.
 * </p>
 * <p>
 * The service provides two main operations:
 * <ul>
 *   <li>Generating redirect URLs to channel-specific manifests</li>
 *   <li>Streaming channel manifest content directly</li>
 * </ul>
 * </p>
 * <p>
 * It interacts with the {@link ChannelRepository} to retrieve channel information
 * and leverages the Quarkus caching system to optimize repeated requests for the
 * same channel data.
 * </p>
 */
@ApplicationScoped
public class ChannelManifestService extends AbstractStreamService {

    private static final Logger LOG = Logger.getLogger(ChannelManifestService.class);
    private static final String MANIFEST_REDIRECT_URL = "/live/%s/%s/%s.mpd";

    @Inject
    private ChannelRepository repository;

    @Inject
    @CacheName("channel")
    Cache cache;

    /**
     * Generates a URL for a channel's DASH manifest.
     * <p>
     * This method constructs a URL path for accessing a channel's manifest by:
     * <ol>
     *   <li>Retrieving the channel information (with caching)</li>
     *   <li>Making an HTTP request to the channel's source URL</li>
     *   <li>Extracting security token and host information from the response</li>
     *   <li>Formatting a URL path for accessing the manifest</li>
     * </ol>
     * </p>
     * <p>
     * The resulting URL follows the format: <code>/live/{host}/{token}/{channel}.mpd</code>
     * where host is Base64 encoded for security.
     * </p>
     *
     * @param channel The channel code identifier
     * @return A {@link Uni} containing the formatted manifest URL path, or an error if the channel is not found
     */
    public Uni<String> getManifestRedirectUrl(String channel) {
        LOG.infof("Channel URL: %s", channel);
        return getChannel(channel)
            .onItem().ifNull().failWith(() -> new NotFoundException("Manifest not found"))
            .flatMap(ch ->
                get(ch.url)
                .onItem().transformToUni(req -> req.send())
                .onFailure().retry().atMost(3)
                .onItem().transform(resp -> {
                    String location = resp.headers().get(HttpHeaders.LOCATION);
                    String token = location.split("/")[3];
                    String host = base64Encoder.encodeToString(location.split("/")[2].getBytes());
                    return String.format(MANIFEST_REDIRECT_URL, host, token, ch.code);
                })
            );
    }

    public Uni<String> getManifestUrl(String host, String token, String channel) {
        return getChannel(channel)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manifest not found"))
                .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
                .map(path -> formatMpdUrl(host, token, channel, path));
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
