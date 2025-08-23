package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.smallrye.mutiny.tuples.Tuple2;
import org.bson.types.ObjectId;
import quantum.video.model.Channel;
import quantum.video.model.Program;

import java.time.ZoneId;

/**
 * Data Transfer Object representing a channel available for playback.
 * <p>
 * Contains channel metadata, streaming URL, DRM info, and the currently playing program.
 * </p>
 *
 * @param id         Unique identifier of the channel
 * @param name       Name of the channel
 * @param number     Channel number
 * @param logo       Logo URL for the channel
 * @param category   Category or genre of the channel
 * @param url        Streaming URL for the channel
 * @param nowPlaying Information about the currently playing program
 * @param drm        DRM information for protected content
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaybackChannel(
        ObjectId id,
        String name,
        Integer number,
        String logo,
        String category,
        String url,
        NowPlaying nowPlaying,
        DrmInfo drm) {


    public PlaybackChannel(Tuple2<Channel, Program> tuple) {
        this(tuple.getItem1(), tuple.getItem2(), null, false);
    }

    public PlaybackChannel(Tuple2<Channel, Program> tuple, String urlPattern, boolean includeDrm) {
        this(tuple.getItem1(), tuple.getItem2(), urlPattern, includeDrm);
    }

    public PlaybackChannel(Channel channel, Program program, String urlPattern, boolean includeDrm) {
        this(
            channel.id,
            channel.name,
            channel.number,
            channel.logo,
            channel.category,
            urlPattern != null
                ? channel.proxy ? String.format(urlPattern, channel.id) : channel.url
                : null,
            program != null ?
                new NowPlaying(
                    program.id,
                    program.title,
                    program.description,
                    program.start.atZone(ZoneId.systemDefault()),
                    program.end.atZone(ZoneId.systemDefault())
                ) : null,
            includeDrm && channel.drm != null ? new DrmInfo(channel.drm) : null
        );
    }

}
