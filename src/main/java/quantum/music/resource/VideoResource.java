package quantum.music.resource;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.core.http.HttpClient;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import quantum.music.model.Channel;

import java.net.URL;
import java.util.Base64;
import java.util.Optional;

@Path("/live")
public class VideoResource {

    private static final Logger LOG = Logger.getLogger(VideoResource.class);

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

    @GET
    @Path("/manifest/{channel}.mpd")
    @Produces("text/html")
    public Uni<Response> manifest(@PathParam("channel") String channel) {
        LOG.infof("Request for manifest: %s", channel);
        Channel c = Channel.findByCode(channel);
        return Optional.ofNullable(c)
                .map(url -> createManifestRedirect(channel, c.url))
                .orElseGet(() -> notFoundResponse(channel));
    }

    @GET
    @Path("/{host}/{token}/{channel}.mpd")
    @Produces("application/dash+xml")
    public Multi<Buffer> mdp(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel) {

        LOG.infof("Request for mpd: %s", channel);
        Channel c = Channel.findByCode(channel);
        String url = "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + extractPathBetweenDomainAndFile(c.url) + channel + ".mpd";
        return stream(url);
    }

    @GET
    @Path("/{host}/{token}/{channel}-mp4a_{file}.mp4")
    @Produces("audio/mp4")
    public Multi<Buffer> audio(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request for video: %s", channel);
        Channel c = Channel.findByCode(channel);
        String url = "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + extractPathBetweenDomainAndFile(c.url) + channel + "-mp4a_" + file + ".mp4";
        return stream(url);
    }

    @GET
    @Path("/{host}/{token}/{channel}-avc1_{file}.mp4")
    @Produces("video/mp4")
    public Multi<Buffer> video(
            @PathParam("host") String host,
            @PathParam("token") String token,
            @PathParam("channel") String channel,
            @PathParam("file") String file) {

        LOG.infof("Request for video: %s", channel);
        Channel c = Channel.findByCode(channel);
        String url = "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + extractPathBetweenDomainAndFile(c.url) + channel + "-avc1_" + file + ".mp4";
        return stream(url);
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
                                )
                        )
                        .onItem().transformToUni(req -> req.send())
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

