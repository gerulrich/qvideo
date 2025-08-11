package quantum.video.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import quantum.video.api.PageResponse;
import quantum.video.api.Paging;
import quantum.video.api.PlaybackProgram;
import quantum.video.service.ProgramService;

/**
 * Resource for managing playback programs for Personal Video Recording (PVR).
 * <p>
 * This resource provides RESTful endpoints to retrieve information about available
 * playback programs for personal video recording, including a paginated list of all
 * programs and detailed information about specific programs by ID.
 * </p>
 * <p>
 * Access to program information is controlled based on user authorization level,
 * which is extracted from the security context.
 * </p>
 */
@Path("/pvr")
@Produces(MediaType.APPLICATION_JSON)
public class PlaybackProgramResource extends AbstractSecureResource {

    private static final Logger LOG = Logger.getLogger(PlaybackProgramResource.class);
    private final String manifestPatternUrl = "http://localhost:8080/pvr/manifest/%s/%s";

    @Inject
    private ProgramService service;

    /**
     * Retrieves a paginated list of playback programs.
     * <p>
     * This endpoint returns a list of playback programs available to the user based on their
     * authorization level. Results are paginated according to the specified page and size parameters.
     * </p>
     * <p>
     * The programs returned are filtered according to the user's permission level,
     * and each program contains basic metadata without full details.
     * </p>
     *
     * @param page The page number to retrieve (1-based indexing, minimum value: 1)
     * @param size The number of items per page (minimum value: 5, default: 10)
     * @param ctx The security context for user authorization level extraction
     * @return A {@link Uni} containing a {@link PageResponse} of {@link PlaybackProgram} objects
     *         with pagination metadata
     */
    @GET
    @Path("/programs")
    public Uni<PageResponse<PlaybackProgram>> getPlaybackPrograms(
            @Valid @Min(1) @QueryParam("page") @DefaultValue("1") int page,
            @Valid @Min(5) @QueryParam("size") @DefaultValue("10") int size,
            @Context SecurityContext ctx
    ) {
        LOG.infof("Request for playback programs: page=%d, size=%d", page, size);
        return service.getPrograms(getUserLevel(ctx), page - 1, size)
                .onItem().transform(data -> new PageResponse<>(
                        data.items().stream().map(PlaybackProgram::new).toList(),
                        Paging.of(data.page() + 1, data.size(), data.elements(), data.total()))
                );
    }

    /**
     * Retrieves a specific playback program by its identifier.
     * <p>
     * This endpoint returns detailed information about a specific playback program,
     * including extended metadata and content availability information. Access to the
     * program information depends on the user's authorization level.
     * </p>
     * <p>
     * If the requested program does not exist or the user lacks sufficient permissions,
     * a 404 Not Found response will be returned.
     * </p>
     *
     * @param id The unique identifier of the playback program
     * @param ctx The security context for user authorization level extraction
     * @return A {@link Uni} containing the requested {@link PlaybackProgram} with detailed information
     * @throws jakarta.ws.rs.NotFoundException if the program is not found or user lacks permission
     */
    @GET
    @Path("/programs/{id}")
    public Uni<PlaybackProgram> getPlaybackProgram(@PathParam("id") String id, @Context SecurityContext ctx) {
        LOG.infof("Request for playback program: %s", id);
        return service.getProgram(id, getUserLevel(ctx))
            .onItem().transform(program -> new PlaybackProgram(program, manifestPatternUrl, true)
        );
    }

}
