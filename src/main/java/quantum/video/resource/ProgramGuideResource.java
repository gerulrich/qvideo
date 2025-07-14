package quantum.video.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import quantum.video.model.ProgramGuideItem;
import quantum.video.service.ProgramGuideService;
import quantum.video.service.TokenService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * REST resource for Electronic Program Guide (EPG) operations.
 * Provides endpoints to retrieve program schedule information for TV channels.
 * Handles authentication via TokenService and delegates business logic to ProgramGuideService.
 */
@Path("/epg")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProgramGuideResource {

    private static final Logger LOG = Logger.getLogger(ProgramGuideResource.class);

    @Inject
    ProgramGuideService programGuideService;

    @Inject
    TokenService tokenService;

    /**
     * Retrieves program schedules for a specified channel.
     * Fetches data from 24 hours in the past to 24 hours in the future.
     *
     * @param channel The channel ID for which to retrieve program schedules
     * @return A Uni emitting a List of ProgramGuideItem objects representing the program schedule
     */
    @GET
    @Path("/programs")
    public Uni<List<ProgramGuideItem>> getProgramSchedules(@QueryParam("channel") @NotNull Integer channel) {
        Instant fromInstant = Instant.now().minus(24, ChronoUnit.HOURS);
        Instant toInstant = Instant.now().plus(24, ChronoUnit.HOURS);

        String dateFrom = String.valueOf(fromInstant.toEpochMilli());
        String dateTo = String.valueOf(toInstant.toEpochMilli());

        LOG.infof("Retrieving program guide data for range: %s to %s and channel %s", dateFrom, dateTo, channel);

        return tokenService.withToken(() -> programGuideService.getProgramGuide(dateFrom, dateTo, Collections.singletonList(channel)));
    }
}
