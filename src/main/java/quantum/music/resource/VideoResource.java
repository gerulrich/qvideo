package quantum.music.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import quantum.music.service.ProxyService;
import quantum.music.service.StreamService;

@Path("/live")
public class VideoResource {

    private static final Logger LOG = Logger.getLogger(VideoResource.class);

    @Inject
    private StreamService streamService;
    @Inject
    private ProxyService proxyService;

    @GET
    @Path("/manifest/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> manifest(@PathParam("channel") String channel, @Context UriInfo uriInfo) {
        LOG.infof("Request for manifest: %s", channel);
        String baseUri = uriInfo.getBaseUri().toString();
        return streamService.getManifestRedirect(baseUri, channel)
            .onItem().transform(url ->
                Response.status(Response.Status.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build()
            )
            .onFailure().invoke(() -> LOG.warnf("Failed to redirect to manifest for channel: %s", channel))
            .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{host}/{token}/{channel}.mpd")
    @Produces("application/dash+xml")
    public Multi<Buffer> mdp(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel) {

        LOG.infof("Request for mpd: %s", channel);
        return proxyService
            .getMPDUrl(host, token, channel)
            .onItem().transformToMulti(streamService::stream);
    }

    @GET
    @Path("/{host}/{token}/{channel}-mp4a_{file}.mp4")
    @Produces("audio/mp4")
    public Multi<Buffer> audio(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request audio %s-mp4a_%s.mp4", channel, file);
        return proxyService
            .getAudioUrl(host, token, channel, file)
            .onItem().transformToMulti(streamService::stream);
    }

    @GET
    @Path("/{host}/{token}/{channel}-avc1_{file}.mp4")
    @Produces("video/mp4")
    public Multi<Buffer> video(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request video %s-avc1_%s.mp4", channel, file);
        return proxyService
                .getVideoUrl(host, token, channel, file)
                .onItem().transformToMulti(streamService::stream);
    }
}
