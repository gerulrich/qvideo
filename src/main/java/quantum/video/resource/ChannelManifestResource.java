package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import quantum.video.service.ChannelManifestService;

import java.net.URI;

/**
 * Resource for handling DASH manifests for live channel streaming.
 * <p>
 * This resource provides endpoints to serve DASH (Dynamic Adaptive Streaming over HTTP)
 * manifests for video streaming, supporting both manifest redirections and direct
 * manifest serving. It acts as a thin controller layer delegating the actual
 * manifest generation logic to {@link ChannelManifestService}.
 * </p>
 * <p>
 * The resource supports the following operations:
 * <ul>
 *   <li>Redirecting to channel-specific manifest files via {@code /live/manifest/{channel}.mpd}</li>
 *   <li>Serving DASH MPD manifest files directly via {@code /live/{host}/{token}/{channel}.mpd}</li>
 * </ul>
 * </p>
 * <p>
 * Response formats:
 * <ul>
 *   <li>Redirect endpoint: HTTP 302 with Location header (or 404 if not found)</li>
 *   <li>Direct manifest endpoint: Streaming {@code application/dash+xml} content via Mutiny {@code Multi<Buffer>}</li>
 * </ul>
 * </p>
 * <p>
 * Note: The actual audio and video segments referenced in these manifests are
 * handled by the ChannelSegmentResource.
 * </p>
 */
@Path("/live")
public class ChannelManifestResource {

    private static final Logger LOG = Logger.getLogger(ChannelManifestResource.class);

    @Inject
    private ChannelManifestService service;

    /**
     * Redirects to the appropriate manifest file for a channel.
     * <p>
     * This endpoint returns a redirect response (HTTP 302) to the actual manifest location,
     * which is determined by the stream service based on the channel ID. If the requested
     * channel does not exist or is not available, a 404 Not Found response is returned.
     * </p>
     *
     * @param channel The channel identifier to fetch the manifest for
     * @return A {@link Uni} containing a redirect {@link Response} to the manifest URL,
     *         or a 404 response if the channel is not found
     */
    @GET
    @Path("/manifest/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> redirect(@PathParam("channel") String channel) {
        LOG.infof("Request for channel manifest (url redirect): %s", channel);
        return service.getManifestRedirectUrl(channel)
            .onFailure().invoke(ex -> LOG.warnf("Failed to redirect to manifest for channel: %s", channel, ex))
            .onItem().ifNull().failWith(() -> new NotFoundException("Channel not found"))
            .onItem().transform(url -> Response.temporaryRedirect(URI.create(url)).build());

    }

    /**
     * Serves the DASH MPD manifest file for streaming.
     * <p>
     * This endpoint streams the DASH Media Presentation Description (MPD) manifest
     * for a specific channel, host and token combination. The manifest contains references
     * to available media segments and their characteristics (bitrates, resolutions, etc.).
     * </p>
     *
     * @param host The host identifier for content source location
     * @param token The security token for authentication and authorization
     * @param channel The channel identifier for which to serve the manifest
     * @return A {@link Multi} stream of {@link Buffer} containing the MPD manifest XML data
     */
    @GET
    @Path("/{host}/{token}/{channel}.mpd")
    @Produces("application/dash+xml")
    public Multi<Buffer> manifest(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel)
    {
        LOG.infof("Request for channel manifest: %s", channel);
        return service.getManifestUrl(host, token, channel)
            .onFailure().invoke(ex -> LOG.warnf("Failed to get manifest URL for channel: %s", channel, ex))
            .onItem().ifNull().failWith(() -> new NotFoundException("Channel not found"))
            .onItem().transformToMulti(service::stream);
    }
}
