package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.video.service.ProgramManifestService;

/**
 * Resource for handling DASH manifests for recorded program playback.
 * <p>
 * This resource provides endpoints to serve DASH (Dynamic Adaptive Streaming over HTTP)
 * manifests for recorded program streaming, supporting both manifest redirections
 * and direct manifest serving. It acts as a thin controller layer delegating the actual
 * manifest generation logic to {@link ProgramManifestService}.
 * </p>
 * <p>
 * The resource is part of the PVR (Personal Video Recording) subsystem and works in
 * conjunction with {@link ProgramSegmentResource} which handles the media segments
 * referenced in these manifests.
 * </p>
 * <p>
 * The resource supports the following operations:
 * <ul>
 *   <li>Redirecting to program-specific manifest files</li>
 *   <li>Serving DASH MPD manifest files directly</li>
 * </ul>
 * </p>
 */
@Path("/pvr")
public class ProgramManifestResource {

    private static final Logger LOG = Logger.getLogger(ProgramManifestResource.class);

    @Inject
    private ProgramManifestService service;

    /**
     * Redirects to the appropriate manifest file for a recorded program.
     * <p>
     * This endpoint returns a redirect response (HTTP 302) to the actual manifest location,
     * which is determined by the service based on the program ID and channel. If the requested
     * program does not exist or is not available, a 404 Not Found response is returned.
     * </p>
     *
     * @param id The unique identifier of the recorded program
     * @param channel The channel identifier related to the program
     * @return A {@link Uni} containing a redirect {@link Response} to the manifest URL,
     *         or a 404 response if the program is not found
     */

    @GET
    @Path("/manifest/{id}/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> redirect(@PathParam("id") ObjectId id, @PathParam("channel") String channel) {
        LOG.infof("Request for program manifest (url redirect): %s", channel);
        return service.getManifestRedirectUrl(id, channel)
            .onFailure().invoke(() -> LOG.warnf("Failed to redirect to program manifest for channel: %s", channel))
            .onItem().transform(url -> Response
                        .status(Response.Status.FOUND)
                        .header(HttpHeaders.LOCATION, url)
                        .build());
    }

    /**
     * Serves the DASH MPD manifest file for recorded program streaming.
     * <p>
     * This endpoint streams the DASH Media Presentation Description (MPD) manifest
     * for a specific recorded program identified by its ID and channel. The manifest
     * contains references to available media segments and their characteristics
     * (bitrates, resolutions, etc.).
     * </p>
     * <p>
     * The path structure includes host and token information for security and routing
     * purposes, following the application's REST API conventions.
     * </p>
     *
     * @param host The host identifier for content source location
     * @param token The security token for authentication and authorization
     * @param id The unique identifier of the recorded program
     * @param channel The channel identifier related to the program
     * @return A {@link Multi} stream of {@link Buffer} containing the MPD manifest XML data
     */
    @GET
    @Path("/{host}/{token}/{id}/a/b/c/d/e/{channel}.mpd")
    @Produces("application/dash+xml")
    public Multi<Buffer> manifest(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") ObjectId id,
            @PathParam("channel") String channel)
    {
        LOG.infof("Request for program manifest: %s", channel);
        return service.getManifestRedirectUrl(host, token, id)
            .onFailure().invoke(ex -> LOG.warnf("Failed to get program manifest URL for channel: %s", channel, ex))
            .onItem().transformToMulti(service::stream);
    }
}
