package quantum.music.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.smallrye.mutiny.tuples.Tuple2;
import org.bson.types.ObjectId;
import quantum.music.model.Channel;
import quantum.music.model.Program;

import java.time.ZoneId;

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
        this(tuple.getItem1(), tuple.getItem2(), false);
    }

    public PlaybackChannel(Tuple2<Channel, Program> tuple, boolean includeDrm) {
        this(tuple.getItem1(), tuple.getItem2(), includeDrm);
    }

    public PlaybackChannel(Channel channel, Program program, boolean includeDrm) {
        this(
            channel.id,
            channel.name,
            channel.number,
            channel.logo,
            channel.category,
            channel.proxy ? String.format("http://localhost:8080/live/manifest/%s.mpd", channel.code) : channel.url,
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
