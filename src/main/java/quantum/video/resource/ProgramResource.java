package quantum.video.resource;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.logging.Logger;
import quantum.video.api.PageResponse;
import quantum.video.api.Paging;
import quantum.video.api.PlaybackProgram;
import quantum.video.service.ProgramService;

import java.security.Principal;

@Path("/pvr")
public class ProgramResource {

    private static final Logger LOG = Logger.getLogger(ProgramResource.class);

    @Inject
    private ProgramService service;

    @GET
    @Path("/programs")
    public Uni<PageResponse<PlaybackProgram>> channels(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @Context SecurityContext ctx
    ) {
        LOG.infof("Request for program: page=%d, size=%d", page, size);
        return service.getPrograms(getUserLevel(ctx), page - 1, size)
                .onItem().transform(data -> new PageResponse<>(
                        data.items().stream().map(program -> new PlaybackProgram(program, false)).toList(),
                        Paging.of(data.page() + 1, data.size(), data.elements(), data.total()))
                );
    }

    @GET
    @Path("/programs/{id}")
    public Uni<PlaybackProgram> channel(@PathParam("id") String id, @Context SecurityContext ctx) {
        LOG.infof("Request for program: %s", id);
        return service.getProgram(id, getUserLevel(ctx))
                .onItem().transform(program -> new PlaybackProgram(program, true));
    }

    private int getUserLevel(SecurityContext ctx) {
        if (ctx.getUserPrincipal() == null) {
            return 0;
        }
        Principal principal = ctx.getUserPrincipal();
        if (principal instanceof JWTCallerPrincipal jwtPrincipal) {
            return jwtPrincipal.claim("level").map(v -> Integer.parseInt(v.toString())).orElse(0);
        }
        return 0;
    }

}

