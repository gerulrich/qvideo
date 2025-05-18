package quantum.music.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.model.Program;

import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.Optional;

@ApplicationScoped
public class PvrService {

    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    public Uni<String> getMPDUrl(String host, String token, String id, String channel) {
        return Optional
            .ofNullable(Program.findById(id))
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatMpdUrl(host, token, channel, path))
            .map(url ->  Uni.createFrom().item(url))
            .orElse(Uni.createFrom().failure(new Exception("Channel not found")));
    }

    public Uni<String> getVideoUrl(String host, String token, String id, String channel, String file) {
        return Optional
            .ofNullable(Program.findById(id))
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatVideoUrl(host, token, path, channel, file))
            .map(url ->  Uni.createFrom().item(url))
            .orElse(Uni.createFrom().failure(new Exception("Channel not found")));
    }

    public Uni<String> getAudioUrl(String host, String token, String id, String channel, String file) {
        return Optional
            .ofNullable(Program.findById(id))
            .flatMap(program -> extractPathBetweenDomainAndFile(program.url))
            .map(path -> formatAudioUrl(host, token, path, channel, file))
            .map(url ->  Uni.createFrom().item(url))
            .orElse(Uni.createFrom().failure(new Exception("Channel not found")));
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

    private Optional<String> extractPathBetweenDomainAndFile(String urlString) {
        return getUrl(urlString).map(url -> {
            String path = url.getPath();
            int lastSlash = path.lastIndexOf("/");
            return path.substring(0, lastSlash + 1);
        });
    }

    private Optional<URL> getUrl(String url) {
        try {
            return Optional.of(new URI(url).toURL());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
