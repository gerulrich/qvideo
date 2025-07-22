package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import quantum.video.service.AbstractStreamService;
import quantum.video.service.ChannelSegmentService;

/**
 * Resource for handling live video streaming segments.
 * <p>
 * This resource provides endpoints for serving audio and video segments for DASH-based
 * live video streaming. It works in conjunction with {@link ChannelManifestResource}
 * which handles the manifest files that reference these segments.
 * </p>
 * <p>
 * The resource handles two primary segment types:
 * <ul>
 *   <li>Audio segments (MP4 audio)</li>
 *   <li>Video segments (MP4 video)</li>
 * </ul>
 * </p>
 * <p>
 * This class extends {@link AbstractSecureResource} to leverage authentication and
 * authorization functionality for protected content streams.
 * </p>
 * <p>
 * Note: The actual segment data is retrieved by the {@link ChannelSegmentService}
 * which this resource delegates to.
 * </p>
 */
@Path("/live")
@Produces(MediaType.APPLICATION_JSON)
public class ChannelSegmentResource extends AbstractStreamService {

    private static final Logger LOG = Logger.getLogger(ChannelSegmentResource.class);

    @Inject
    private ChannelSegmentService service;

    /**
     * Serves audio segments for DASH streaming.
     * <p>
     * This endpoint streams audio segments in MP4 format for adaptive bitrate streaming.
     * The segments are identified by a combination of host, security token, channel,
     * and segment file identifier.
     * </p>
     * <p>
     * The endpoint uses non-blocking I/O via Mutiny's Multi to efficiently stream
     * potentially large segment data.
     * </p>
     *
     * @param host The host identifier representing the content source
     * @param token The security token for authentication and authorization
     * @param channel The channel identifier for which audio is being requested
     * @param file The specific audio segment identifier (typically includes initialization or sequence information)
     * @return A {@link Multi} stream of {@link Buffer} containing the audio segment data in MP4 format
     */
    @GET
    @Path("/{host}/{token}/{channel}-mp4a_{file}.mp4")
    @Produces("audio/mp4")
    public Multi<Buffer> audio(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {
        LOG.infof("Request for audio segment: channel=%s, file=%s", channel, file);
        return service.getAudioSegment(host, token, channel, file)
            .onFailure().invoke(ex -> LOG.warnf("Failed to get audio segment for channel: %s", channel, ex))
            .onItem().ifNull().failWith(() -> new NotFoundException("Video segment not found"))
            .onItem().transformToMulti(service::stream);
    }

    /**
     * Serves video segments for DASH streaming.
     * <p>
     * This endpoint streams video segments in MP4 format for adaptive bitrate streaming.
     * The segments are identified by a combination of host, security token, channel,
     * and segment file identifier.
     * </p>
     * <p>
     * The endpoint uses non-blocking I/O via Mutiny's Multi to efficiently stream
     * potentially large segment data. The video segments contain H.264 (AVC1) encoded content.
     * </p>
     *
     * @param host The host identifier representing the content source
     * @param token The security token for authentication and authorization
     * @param channel The channel identifier for which video is being requested
     * @param file The specific video segment identifier (typically includes quality level and sequence information)
     * @return A {@link Multi} stream of {@link Buffer} containing the video segment data in MP4 format
     */
    @GET
    @Path("/{host}/{token}/{channel}-avc1_{file}.mp4")
    @Produces("video/mp4")
    public Multi<Buffer> video(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request for video segment: channel=%s, file=%s", channel, file);
        return service.getVideoSegment(host, token, channel, file)
                .onFailure().invoke(ex -> LOG.warnf("Failed to get video segment for channel: %s", channel, ex))
                .onItem().ifNull().failWith(() -> new NotFoundException("Video segment not found"))
                .onItem().transformToMulti(service::stream);
    }
}
