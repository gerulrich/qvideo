package quantum.music.resource;

import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.net.URL;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/live")
public class VideoResource {

    private static final Logger LOG = Logger.getLogger(VideoResource.class);

    private final Map<String, String> channels = Stream.of(new String[][]{
            {"Cinemax", "https://cdn.cvattv.com.ar/live/c6eds/Cinemax/SA_Live_dash_enc_2A/Cinemax.mpd"},
            {"ESPN2HD", "https://cdn.cvattv.com.ar/live/c3eds/ESPN2HD/SA_Live_dash_enc_2A/ESPN2HD.mpd"},
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    @Inject
    private Vertx vertx;
    private HttpClient httpClient;
    private Base64.Encoder base64Encoder;
    private Base64.Decoder base64Decoder;

    @PostConstruct
    void init() {
        httpClient = vertx.createHttpClient();
        base64Encoder = Base64.getEncoder();
        base64Decoder = Base64.getDecoder();
    }

    private static final String DOMAIN = "http://localhost:8080/live/";
    private static final String MANIFEST_URL = DOMAIN + "{host}/{token}/{channel}.mpd";
    private static final String LOCATION_HEADER = "Location";
    private static final String USER_AGENT = "okhttp/4.12.0";
    private static final String CORS_HEADER = "Access-Control";

    @GET
    @Path("/manifest/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> manifest(@PathParam("channel") String channel) {
        LOG.infof("Request for manifest: %s", channel);
        return Optional.ofNullable(channels.get(channel))
                .map(url -> createManifestRedirect(channel, url))
                .orElseGet(() -> notFoundResponse(channel));
    }

    @GET
    @Path("/{host}/{token}/{channel}.mpd")
    public Uni<Response> mdp(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel, @Context RoutingContext rc) {

        LOG.infof("Request for mpd: %s", channel);
        String channelUrl = channels.get(channel);
        String url = "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + extractPathBetweenDomainAndFile(channelUrl) + channel + ".mpd";
        return stream(url, rc);
    }

    @GET
    @Path("/{host}/{token}/{channel}-{file}.mp4")
    public Uni<Response> data(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file,
            @Context RoutingContext rc) {

        LOG.infof("Request for video: %s", channel);
        String channelUrl = channels.get(channel);
        String url = "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + extractPathBetweenDomainAndFile(channelUrl) + channel + "-" + file + ".mp4";
        return stream(url, rc);
    }

    private Uni<Response> createManifestRedirect(String channel, String url) {
        LOG.infof("Channel URL: %s", url);
        return httpClient.request(
                        new RequestOptions()
                                .setMethod(HttpMethod.GET)
                                .setAbsoluteURI(url)
                                .setHeaders(
                                        MultiMap.caseInsensitiveMultiMap()
                                                .add("User-Agent", USER_AGENT)
                                                .add("X-Flow-Origin", "AndroidTV")
                                )
                )
                .onItem().transformToUni(req -> req.send())
                .onItem().transform(resp -> {
                    LOG.info("Request sent to: " + url);
                    String location = resp.headers().get("Location");
                    String token = location.split("/")[3];
                    String host = base64Encoder.encodeToString(location.split("/")[2].getBytes());
                    String manifestUrl = MANIFEST_URL
                            .replace("{host}", host)
                            .replace("{token}", token)
                            .replace("{channel}", channel);

                    return Response.status(Response.Status.FOUND)
                            .header(LOCATION_HEADER, manifestUrl).build();
                });
    }

    private Uni<Response> notFoundResponse(String channel) {
        LOG.warnf("Channel not found: %s", channel);
        return Uni.createFrom().item(Response.status(Response.Status.NOT_FOUND).build());
    }


    public static String extractPathBetweenDomainAndFile(String urlString) {
        try {
            URL url = new URL(urlString);
            String path = url.getPath();
            int lastSlash = path.lastIndexOf("/");
            return path.substring(0, lastSlash + 1);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uni<Response> stream(String url, RoutingContext rc) {
        return httpClient.request(new RequestOptions()
                .setMethod(HttpMethod.GET)
                        .setAbsoluteURI(url)
                        .setHeaders(
                                MultiMap.caseInsensitiveMultiMap()
                                        .add("User-Agent", USER_AGENT)
                                        .add("X-Flow-Origin", "AndroidTV")
                        ))
                .onItem().transformToUni(req -> req.send())
                .onItem().transformToUni(resp -> resp.body().map(buffer -> {
                        if (resp.statusCode() == 200) {
                            Response.ResponseBuilder builder = Response.ok(buffer);
                            resp.headers().forEach(header -> {
                                String key = header.getKey();
                                if (!key.equalsIgnoreCase("Content-Length") &&
                                    !key.equalsIgnoreCase("Transfer-Encoding") &&
                                    !key.startsWith(CORS_HEADER)) {
                                    builder.header(key, header.getValue());
                                }
                            });
                            return builder.build();
                        } else {
                            return Response.status(resp.statusCode()).build();
                        }
                }));
    }
}

