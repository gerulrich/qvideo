package quantum.music.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.types.ObjectId;
import quantum.music.model.Program;
import quantum.music.repository.ProgramRepository;

import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.function.Function;

@ApplicationScoped
public class PvrService {

    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    @Inject
    @CacheName("program")
    Cache cache;

    @Inject
    private ProgramRepository repository;

    private Uni<Program> getProgram(String id) {
        return cache.get(id, k -> repository.findById(new ObjectId(k)))
                .flatMap(Function.identity());
    }

    public Uni<String> getMPDUrl(String host, String token, String id, String channel) {
        return getProgram(id)
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatMpdUrl(host, token, channel, path));
    }

    public Uni<String> getVideoUrl(String host, String token, String id, String channel, String file) {
        return getProgram(id)
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatVideoUrl(host, token, path, channel, file));
    }

    public Uni<String> getAudioUrl(String host, String token, String id, String channel, String file) {
        return getProgram(id)
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatAudioUrl(host, token, path, channel, file));
    }

    private String formatMpdUrl(String host, String token, String channel, String path) {
        return "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + ".mpd";
    }

    private String formatVideoUrl(String host, String token, String path, String channel, String file) {
        return  "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + "-avc1_" + file + ".mp4";
    }

    private String formatAudioUrl(String host, String token, String path, String channel, String file) {
        return  "https://" + new String(base64Decoder.decode(host)) + "/" +
                token + path + channel + "-mp4a_" + file + ".mp4";
    }

    private Uni<String> extractPathBetweenDomainAndFile(String urlString) {
        return getUrl(urlString).map(url -> {
            String path = url.getPath();
            int lastSlash = path.lastIndexOf("/");
            return path.substring(0, lastSlash + 1);
        });
    }

    private Uni<URL> getUrl(String url) {
        try {
            return Uni.createFrom().item(new URI(url).toURL());
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }

}
