package quantum.music.service;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import quantum.music.model.Channel;

import java.net.URL;
import java.util.Base64;

@ApplicationScoped
public class ProxyService {

    private Base64.Decoder base64Decoder;

    @PostConstruct
    void init() {
        base64Decoder = Base64.getDecoder();
    }

    public Uni<String> getMPDUrl(String host, String token, String channel) {
        return Uni.createFrom().item(() -> {
            Channel c = Channel.findByCode(channel);
            return "https://" + new String(base64Decoder.decode(host)) + "/" +
                    token + extractPathBetweenDomainAndFile(c.url) + channel + ".mpd";
        });
    }

    public Uni<String> getAudioUrl(String host, String token, String channel, String file) {
        return Uni.createFrom().item(() -> {
            Channel c = Channel.findByCode(channel);
            return "https://" + new String(base64Decoder.decode(host)) + "/" +
                    token + extractPathBetweenDomainAndFile(c.url) + channel + "-mp4a_" + file + ".mp4";
        });
    }

    public Uni<String> getVideoUrl(String host, String token, String channel, String file) {
        return Uni.createFrom().item(() -> {
            Channel c = Channel.findByCode(channel);
            return "https://" + new String(base64Decoder.decode(host)) + "/" +
                    token + extractPathBetweenDomainAndFile(c.url) + channel + "-avc1_" + file + ".mp4";
        });
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
}
