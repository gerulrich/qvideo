package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import quantum.video.service.PvrService;
import quantum.video.service.StreamService;

@Path("/pvr")
public class PvrVideoResource {

    private static final Logger LOG = Logger.getLogger(PvrVideoResource.class);

    @Inject
    private StreamService streamService;

    @Inject
    private PvrService pvrService;

    @GET
    @Path("/manifest/{id}/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> manifest(@PathParam("id") String id, @PathParam("channel") String channel) {
        LOG.infof("Request for manifest: %s", channel);
        return streamService.getPvrManifestUrl(id, channel)
            .onItem().transform(url ->
                Response.status(Response.Status.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build()
            )
            .onFailure().invoke(() -> LOG.warnf("Failed to redirect to manifest for channel: %s", channel))
            .onItem().ifNull().continueWith(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/{host}/{token}/{id}/a/b/c/d/e/{channel}.mpd")
    @Produces("application/dash+xml")
    public Multi<Buffer> mdp(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") String id,
            @PathParam("channel") String channel) {

        LOG.infof("Request for mpd: %s", channel);
        return pvrService
            .getMPDUrl(host, token, id, channel)
            .onItem().transformToMulti(streamService::stream);
    }

    @GET
    @Path("/{host}/{token}/{id}/{a}/{b}/{c}/{d}/{e}/{channel}-mp4a_{file}.mp4")
    @Produces("audio/mp4")
    public Multi<Buffer> audio(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") String id,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request audio %s-mp4a_%s.mp4", channel, file);
        return pvrService
            .getAudioUrl(host, token, id, channel, file)
            .onItem().transformToMulti(streamService::stream);
    }

    @GET
    @Path("/{host}/{token}/{id}/{a}/{b}/{c}/{d}/{e}/{channel}-avc1_{file}.mp4")
    @Produces("video/mp4")
    public Multi<Buffer> video(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") String id,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request video %s-avc1_%s.mp4", channel, file);
        return pvrService
                .getVideoUrl(host, token, id, channel, file)
                .onItem().transformToMulti(streamService::stream);
    }
}
