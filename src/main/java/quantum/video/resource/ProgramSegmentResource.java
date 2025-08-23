package quantum.video.resource;

import io.smallrye.mutiny.Multi;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.jboss.logging.Logger;
import quantum.video.service.ProgramSegmentService;

/**
 * Resource for handling recorded program video streaming segments.
 * <p>
 * This resource provides endpoints for serving audio and video segments for DASH-based
 * playback of recorded programs from the PVR (Personal Video Recording) subsystem. It works
 * in conjunction with {@link ProgramManifestResource} which handles the manifest files
 * that reference these segments.
 * </p>
 * <p>
 * The resource handles two primary segment types:
 * <ul>
 *   <li>Audio segments (MP4 audio)</li>
 *   <li>Video segments (MP4 video)</li>
 * </ul>
 * </p>
 * <p>
 * Note: The actual segment data is retrieved by the {@link ProgramSegmentService}
 * which this resource delegates to for business logic.
 * </p>
 */
@Path("/pvr")
public class ProgramSegmentResource {

    private static final Logger LOG = Logger.getLogger(ProgramSegmentResource.class);

    @Inject
    private ProgramSegmentService service;

    /**
     * Serves audio segments for recorded program playback via DASH streaming.
     * <p>
     * This endpoint streams audio segments in MP4 format for adaptive bitrate streaming
     * of recorded programs. The segments are identified by a combination of host, security token,
     * program ID, channel, and segment file identifier.
     * </p>
     * <p>
     * The path structure includes several placeholder segments (a, b, c, d, e) which are
     * part of the URL pattern for consistent routing within the application.
     * </p>
     * <p>
     * The endpoint uses non-blocking I/O via Mutiny's Multi to efficiently stream
     * potentially large segment data.
     * </p>
     *
     * @param host The host identifier representing the content source
     * @param token The security token for authentication and authorization
     * @param id The unique identifier of the recorded program
     * @param channel The channel identifier associated with the program
     * @param file The specific audio segment identifier (typically includes initialization or sequence information)
     * @return A {@link Multi} stream of {@link Buffer} containing the audio segment data in MP4 format
     */
    @GET
    @Path("/{host}/{token}/{id}/{a}/{b}/{c}/{d}/{e}/{channel}-mp4a_{file}.mp4")
    @Produces("audio/mp4")
    public Multi<Buffer> audio(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") String id,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request audio segment for program: %s-mp4a_%s.mp4", channel, file);
        return service.getAudioSegment(host, token, id, file)
            .onFailure().invoke(ex -> LOG.warnf("Failed to get audio segment for channel: %s", channel, ex))
            .onItem().transformToMulti(service::stream);
    }

    /**
     * Serves video segments for recorded program playback via DASH streaming.
     * <p>
     * This endpoint streams video segments in MP4 format for adaptive bitrate streaming
     * of recorded programs. The segments are identified by a combination of host, security token,
     * program ID, channel, and segment file identifier.
     * </p>
     * <p>
     * The path structure includes several placeholder segments (a, b, c, d, e) which are
     * part of the URL pattern for consistent routing within the application.
     * </p>
     * <p>
     * The endpoint uses non-blocking I/O via Mutiny's Multi to efficiently stream
     * potentially large segment data. The video segments contain H.264 (AVC1) encoded content.
     * </p>
     *
     * @param host The host identifier representing the content source
     * @param token The security token for authentication and authorization
     * @param id The unique identifier of the recorded program
     * @param channel The channel identifier associated with the program
     * @param file The specific video segment identifier (typically includes quality level and sequence information)
     * @return A {@link Multi} stream of {@link Buffer} containing the video segment data in MP4 format
     */
    @GET
    @Path("/{host}/{token}/{id}/{a}/{b}/{c}/{d}/{e}/{channel}-avc1_{file}.mp4")
    @Produces("video/mp4")
    public Multi<Buffer> video(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("id") String id,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request video segment for program: %s-avc1_%s.mp4", channel, file);
        return service.getVideoSegment(host, token, id, file)
            .onFailure().invoke(ex -> LOG.warnf("Failed to get video segment for program: %s", channel, ex))
            .onItem().transformToMulti(service::stream);
    }
}
