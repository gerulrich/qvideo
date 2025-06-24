package quantum.video.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import org.bson.types.ObjectId;
import org.jboss.logging.Logger;
import quantum.video.repository.ChannelRepository;
import quantum.video.repository.ProgramRepository;

import java.util.Base64;

@ApplicationScoped
public class StreamService {

    private static final Logger LOG = Logger.getLogger(StreamService.class);
    private static final String MANIFEST_REDIRECT_URL = "/live/%s/%s/%s.mpd";

    private static final String PVR_MANIFEST_URL = "/pvr/%s/%s/%s/a/b/c/d/e/%s.mpd";
    private static final String USER_AGENT = "okhttp/4.12.0";

    @Inject
    private Vertx vertx;
    private HttpClient httpClient;
    private Base64.Encoder base64Encoder;

    @Inject
    private ChannelRepository repository;

    @Inject
    private ProgramRepository programRepository;

    @PostConstruct
    void init() {
        httpClient = vertx.createHttpClient();
        base64Encoder = Base64.getEncoder();
    }

    public Uni<String> getPvrManifestUrl(String id, String channel) {
        LOG.infof("Channel URL: %s", channel);
        return programRepository.findById(new ObjectId(id))
                .flatMap(program -> httpClient.request(
                                new RequestOptions()
                                        .setMethod(HttpMethod.GET)
                                        .setAbsoluteURI(program.url)
                                        .setHeaders(
                                                MultiMap.caseInsensitiveMultiMap()
                                                        .add("User-Agent", USER_AGENT)
                                                        .add("X-Flow-Origin", "AndroidTV")
                                        )
                        )
                        .onItem().transformToUni(req -> req.send())
                        .onFailure().retry().atMost(3)
                        .onItem().transform(resp -> {
                            String location = resp.headers().get(HttpHeaders.LOCATION);
                            String token = location.split("/")[3];
                            String host = base64Encoder.encodeToString(location.split("/")[2].getBytes());
                            return String.format(PVR_MANIFEST_URL, host, token, id, channel);
                        }));
    }

    public Uni<String> getManifestRedirect(String channel) {
        LOG.infof("Channel URL: %s", channel);
        return repository.findByCode(channel).flatMap(c ->
                httpClient.request(
                        new RequestOptions()
                                .setMethod(HttpMethod.GET)
                                .setAbsoluteURI(c.url)
                                .setHeaders(
                                        MultiMap.caseInsensitiveMultiMap()
                                                .add("User-Agent", USER_AGENT)
                                                .add("X-Flow-Origin", "AndroidTV")
                                )
                )
                .onItem().transformToUni(req -> req.send())
                .onFailure().retry().atMost(3)
                .onItem().transform(resp -> {
                    String location = resp.headers().get(HttpHeaders.LOCATION);
                    String token = location.split("/")[3];
                    String host = base64Encoder.encodeToString(location.split("/")[2].getBytes());
                    return String.format(MANIFEST_REDIRECT_URL, host, token, c.code);
                })
        );
    }

    /**
     * Proxies a file from a given URL.
     *
     * @param url The URL of the file to proxy
     * @return A Multi emitting the file's content as Buffer
     */
    public Multi<Buffer> stream(String url) {
        return Multi.createFrom().emitter(emitter -> {
            httpClient
                .request(new RequestOptions()
                    .setMethod(HttpMethod.GET)
                    .setAbsoluteURI(url)
                    .setHeaders(
                        MultiMap.caseInsensitiveMultiMap()
                            .add("User-Agent", USER_AGENT)
                            .add("X-Flow-Origin", "AndroidTV")
                ))
                .onItem().transformToUni(req -> req.send())
                .onFailure().retry().atMost(3)
                .subscribe().with(resp -> {
                    if (resp.statusCode() != 200) {
                        emitter.fail(new WebApplicationException("Failed: " + resp.statusCode(), resp.statusCode()));
                        return;
                    }
                    resp.handler(emitter::emit);
                    resp.endHandler(emitter::complete);
                    resp.exceptionHandler(emitter::fail);
                }, emitter::fail);
        });
    }
}
