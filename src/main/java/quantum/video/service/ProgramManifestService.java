package quantum.video.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.video.model.Program;
import quantum.video.repository.ProgramRepository;

import java.util.function.Function;

/**
 * Service for handling manifest operations for recorded programs (PVR content).
 * <p>
 * This service extends {@link AbstractStreamService} to provide specific functionality
 * for retrieving and generating DASH manifests for recorded programs. It works in
 * conjunction with {@link ProgramSegmentService} which handles the media segments
 * referenced in these manifests.
 * </p>
 * <p>
 * The service utilizes Quarkus caching to optimize program data retrieval and
 * leverages Mutiny's reactive types for efficient non-blocking I/O operations
 * when streaming manifest content.
 * </p>
 * <p>
 * This service supports the following operations:
 * <ul>
 *   <li>Generating redirect URLs to program-specific manifests</li>
 *   <li>Streaming program manifest content directly</li>
 * </ul>
 * </p>
 */
@ApplicationScoped
public class ProgramManifestService extends AbstractStreamService {

    /**
     * Logger instance for this service.
     */
    private static final Logger LOG = Logger.getLogger(ProgramManifestService.class);

    /**
     * URL pattern for PVR manifest paths.
     * The format parameters are: host, token, program id, and channel code.
     */
    private static final String PVR_MANIFEST_URL = "/pvr/%s/%s/%s/a/b/c/d/e/%s.mpd";

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
     * Streams the DASH MPD manifest file for a recorded program.
     * <p>
     * This method retrieves and streams a program's DASH manifest by:
     * <ol>
     *   <li>Retrieving the program information (with caching)</li>
     *   <li>Extracting the path between domain and file from the program URL</li>
     *   <li>Formatting the complete MPD URL using the provided host, token, and channel</li>
     *   <li>Streaming the manifest content using the reactive framework</li>
     * </ol>
     * </p>
     * <p>
     * The method leverages Mutiny's {@link Multi} for reactive streaming of the
     * manifest content with proper back-pressure handling.
     * </p>
     *
     * @param host The Base64-encoded host name
     * @param token The security token for authorization
     * @param id The unique identifier of the recorded program
     * @return A {@link Multi} emitting the manifest content as {@link Buffer} chunks
     */
    public Uni<String> getManifestRedirectUrl(String host, String token, ObjectId id) {
        return getProgram(id)
                .onItem().ifNull().failWith(() -> new NotFoundException("Manifest not found"))
                .map(program -> {
                    String basePath = getBasePath(program.url);
                    return formatMpdUrl(host, token, getChannelCodeFromUrl(program.url), basePath);
                });
    }

    /**
     * Generates a URL for a recorded program's DASH manifest.
     * <p>
     * This method constructs a URL path for accessing a program's manifest by:
     * <ol>
     *   <li>Retrieving the program information (with caching)</li>
     *   <li>Making an HTTP request to the program's source URL</li>
     *   <li>Extracting security token and host information from the response</li>
     *   <li>Formatting a URL path for accessing the manifest</li>
     * </ol>
     * </p>
     * <p>
     * The resulting URL follows the format defined in PVR_MANIFEST_URL constant,
     * where host is Base64 encoded for security.
     * </p>
     *
     * @param id The unique identifier of the recorded program
     * @param channel The channel identifier related to the program
     * @return A {@link Uni} containing the formatted manifest URL path, or an error if the program is not found
     */
    public Uni<String> getManifestRedirectUrl(ObjectId id, String channel) {
        LOG.infof("Channel URL: %s", channel);
        return getProgram(id)
            .onItem().ifNull().failWith(() -> new NotFoundException("Manifest not found"))
            .flatMap(program ->
                get(program.url)
                .onItem().transformToUni(req -> req.send())
                .onFailure().retry().atMost(3)
                .onItem().transform(resp -> {
                    String location = resp.headers().get(HttpHeaders.LOCATION);
                    String token = location.split("/")[3];
                    String host = base64Encoder.encodeToString(location.split("/")[2].getBytes());
                    return String.format(PVR_MANIFEST_URL, host, token, id, channel);
                }));
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
    private Uni<Program> getProgram(ObjectId id) {
        return cache.get(id, k -> repository.findById(k)).flatMap(Function.identity());
    }
}
