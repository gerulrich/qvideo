package quantum.video.service;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import io.vertx.mutiny.core.http.HttpClientRequest;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/**
 * Default implementation of the {@link StreamStrategy} interface.
 * <p>
 * This implementation provides standard HTTP streaming functionality using
 * Vert.x HTTP client. It handles request preparation with appropriate headers
 * and implements efficient content streaming with retry capabilities.
 * </p>
 */
@ApplicationScoped
public class DefaultStreamStrategy implements StreamStrategy {

    /**
     * User agent string used for HTTP requests to streaming endpoints.
     */
    protected static final String USER_AGENT = "okhttp/4.12.0";

    /**
     * Identifier for Android TV client platform.
     */
    protected static final String ANDROID_TV = "AndroidTV";

    /**
     * Vert.x instance for reactive operations.
     */
    @Inject
    protected Vertx vertx;

    /**
     * HTTP client used for making streaming requests.
     */
    protected HttpClient httpClient;

    @PostConstruct
    public void init() {
        this.httpClient = vertx.createHttpClient();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uni<HttpClientRequest> get(String url) {
        return httpClient.request(
            new RequestOptions()
                .setMethod(HttpMethod.GET)
                .setAbsoluteURI(url)
                .setHeaders(
                    MultiMap.caseInsensitiveMultiMap()
                        .add("User-Agent", USER_AGENT)
                        .add("X-Flow-Origin", ANDROID_TV)
                )
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Multi<Buffer> stream(String url) {
        return Multi.createFrom().emitter(emitter -> {
            get(url)
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
