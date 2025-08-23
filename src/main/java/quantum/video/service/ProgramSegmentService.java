package quantum.video.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.bson.types.ObjectId;
import quantum.video.model.Program;
import quantum.video.repository.ProgramRepository;

import java.util.function.Function;

/**
 * Service for handling media segment operations for recorded program playback (PVR content).
 * <p>
 * This service extends {@link AbstractStreamService} to provide specific functionality
 * for retrieving and streaming audio and video segments for recorded programs. It works
 * in conjunction with {@link ProgramManifestService} which handles the manifest files
 * that reference these segments.
 * </p>
 * <p>
 * The service utilizes Quarkus caching to optimize program data retrieval and
 * leverages Mutiny's reactive types for efficient non-blocking I/O operations
 * when streaming potentially large media segments.
 * </p>
 * <p>
 * This service supports the following operations:
 * <ul>
 *   <li>Streaming video segments for recorded programs</li>
 *   <li>Streaming audio segments for recorded programs</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class ProgramSegmentService extends AbstractStreamService {

    /**
     * Cache for program information to avoid repeated database queries.
     */
    @Inject
    @CacheName("program")
    Cache cache;

    /**
     * Repository for retrieving program information.
     */
    @Inject
    private ProgramRepository repository;

    /**
     * Streams a video segment for a recorded program.
     * <p>
     * This method retrieves and streams a video segment by:
     * <ol>
     *   <li>Retrieving the program information (with caching)</li>
     *   <li>Extracting the path between domain and file from the program URL</li>
     *   <li>Formatting the complete video segment URL</li>
     *   <li>Streaming the video content using the reactive framework</li>
     * </ol>
     * </p>
     * <p>
     * The method leverages Mutiny's {@link Multi} for reactive streaming of the
     * video content with proper back-pressure handling. The video segments typically
     * contain H.264 (AVC1) encoded content.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param id The unique identifier of the recorded program
     * @param file The specific video segment file identifier
     * @return A {@link Multi} emitting the video segment content as {@link Buffer} chunks
     */
    public Uni<String> getVideoSegment(String host, String token, String id, String file) {
        return getProgram(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Video segment not found"))
            .map(program -> {
                String basePath = getBasePath(program.url);
                return formatVideoUrl(host, token, basePath, getChannelCodeFromUrl(program.url), file);
            });
    }

    /**
     * Streams an audio segment for a recorded program.
     * <p>
     * This method retrieves and streams an audio segment by:
     * <ol>
     *   <li>Retrieving the program information (with caching)</li>
     *   <li>Extracting the path between domain and file from the program URL</li>
     *   <li>Formatting the complete audio segment URL</li>
     *   <li>Streaming the audio content using the reactive framework</li>
     * </ol>
     * </p>
     * <p>
     * The method leverages Mutiny's {@link Multi} for reactive streaming of the
     * audio content with proper back-pressure handling.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param id The unique identifier of the recorded program
     * @param file The specific audio segment file identifier
     * @return A {@link Multi} emitting the audio segment content as {@link Buffer} chunks
     */
    public Uni<String> getAudioSegment(String host, String token, String id, String file) {
        return getProgram(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Audio segment not found"))
            .map(program -> {
                String basePath = getBasePath(program.url);
                return formatAudioUrl(host, token, basePath, getChannelCodeFromUrl(program.url), file);
            });
    }

    /**
     * Retrieves program information with caching support.
     * <p>
     * This private helper method retrieves program information by its ID,
     * using the Quarkus cache system to optimize repeated requests for the same program.
     * If the program is not in cache, it's retrieved from the repository and then cached.
     * </p>
     *
     * @param id The unique identifier of the program to retrieve
     * @return A {@link Uni} containing the {@link Program} information, or an error if not found
     */
    private Uni<Program> getProgram(String id) {
        return cache.get(id, k -> repository.findById(new ObjectId(k))).flatMap(Function.identity());
    }
}
