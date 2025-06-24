package quantum.video.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import quantum.video.model.Channel;
import quantum.video.repository.ChannelRepository;

import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.function.Function;

@ApplicationScoped
public class LiveService {

    private final Base64.Decoder base64Decoder = Base64.getDecoder();

    @Inject
    @CacheName("channel")
    Cache cache;

    @Inject
    ChannelRepository repository;

    private Uni<Channel> getChannel(String channel) {
        return cache.get(channel, k -> repository.findByCode(k))
            .flatMap(Function.identity());
    }

    public Uni<String> getMPDUrl(String host, String token, String channel) {
        return getChannel(channel)
            .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
            .map(path -> formatMpdUrl(host, token, channel, path));
    }

    public Uni<String> getVideoUrl(String host, String token, String channel, String file) {
        return getChannel(channel)
            .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
            .map(path -> formatVideoUrl(host, token, path, channel, file));
    }

    public Uni<String> getAudioUrl(String host, String token, String channel, String file) {
        return getChannel(channel)
            .flatMap(ch -> extractPathBetweenDomainAndFile(ch.url))
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
