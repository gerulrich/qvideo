package quantum.video.resource;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.Context;
import org.jboss.logging.Logger;
import quantum.video.api.PlaybackChannel;
import quantum.video.api.PageResponse;
import quantum.video.api.Paging;
import quantum.video.service.ChannelService;

import java.security.Principal;

@Path("/live")
public class ChannelResource {

    private static final Logger LOG = Logger.getLogger(ChannelResource.class);

    @Inject
    private ChannelService service;

    @GET
    @Path("/channels")
    public Uni<PageResponse<PlaybackChannel>> channels(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @Context SecurityContext ctx
    ) {
        LOG.infof("Request for channels: page=%d, size=%d", page, size);
        return service.getPlaybackChannels(getUserLevel(ctx), page - 1, size)
                .onItem().transform(data -> new PageResponse<>(
                        data.items().stream().map(PlaybackChannel::new).toList(),
                        Paging.of(data.page() + 1, data.size(), data.elements(), data.total()))
                );
    }

    @GET
    @Path("/channels/{id}")
    public Uni<PlaybackChannel> channel(@PathParam("id") String id, @Context SecurityContext ctx) {
        LOG.infof("Request for channel: %s", id);
        return service.getPlaybackChannel(id, getUserLevel(ctx))
                .onItem().transform(tuple -> new PlaybackChannel(tuple, true));
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

