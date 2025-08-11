package quantum.video.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;
import quantum.video.api.PlaybackChannel;
import quantum.video.api.PageResponse;
import quantum.video.api.Paging;
import quantum.video.service.PlaybackChannelService;

/**
 * Resource for managing playback channels.
 * <p>
 * This resource provides endpoints to retrieve information about live playback channels,
 * including a paginated list of all available channels and details of specific channels.
 * Access to channels is controlled based on user authorization level.
 * </p>
 */
@Path("/live")
public class PlaybackChannelResource extends AbstractSecureResource {

    private static final Logger LOG = Logger.getLogger(PlaybackChannelResource.class);
    private final String manifestUrlPattern = "http://localhost:8080/live/manifest/%s.mpd";

    @Inject
    private PlaybackChannelService service;

    /**
     * Retrieves a paginated list of playback channels.
     * <p>
     * This endpoint returns a list of playback channels available to the user based on their
     * authorization level. Results are paginated according to the specified page and size parameters.
     * Access is restricted based on user permissions.
     * </p>
     *
     * @param page The page number to retrieve (1-based indexing, minimum value: 1)
     * @param size The number of items per page (minimum value: 5, default: 10)
     * @param ctx The security context for user authorization level extraction
     * @return A {@link Uni} containing a {@link PageResponse} of {@link PlaybackChannel} objects
     *         with pagination metadata
     */
    @GET
    @Path("/channels")
    public Uni<PageResponse<PlaybackChannel>> getPlaybackChannels(
            @Valid @Min(1) @QueryParam("page") @DefaultValue("1") int page,
            @Valid @Min(5) @QueryParam("size") @DefaultValue("10") int size,
            @Context SecurityContext ctx
    ) {
        LOG.infof("Request for playback channels: page=%d, size=%d", page, size);
        return service.getPlaybackChannels(getUserLevel(ctx), page - 1, size)
                .onItem().transform(data -> new PageResponse<>(
                        data.items().stream().map(PlaybackChannel::new).toList(),
                        Paging.of(data.page() + 1, data.size(), data.elements(), data.total()))
                );
    }

    /**
     * Retrieves a specific playback channel by its identifier.
     * <p>
     * This endpoint returns detailed information about a specific playback channel,
     * including its current program information. Access to the channel information
     * depends on the user's authorization level.
     * </p>
     * <p>
     * If the requested channel does not exist or the user lacks sufficient permissions,
     * a 404 Not Found response will be returned.
     * </p>
     *
     * @param id The unique identifier of the playback channel
     * @param ctx The security context for user authorization level extraction
     * @return A {@link Uni} containing the requested {@link PlaybackChannel} with detailed program information
     * @throws jakarta.ws.rs.NotFoundException if the channel is not found or user lacks permission
     */
    @GET
    @Path("/channels/{id}")
    public Uni<PlaybackChannel> getPlaybackChannel(@PathParam("id") String id, @Context SecurityContext ctx) {
        LOG.infof("Request for playback channel: %s", id);
        return service.getPlaybackChannel(id, getUserLevel(ctx))
            .onItem().transform(tuple -> new PlaybackChannel(tuple, manifestUrlPattern, true));
    }
}
