package quantum.video.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.Instant;

@MongoEntity(collection="programs")
public class Program {
    public ObjectId id;
    public String title;
    public String description;
    @BsonProperty("episode_title")
    public String episode;
    public String url;
    public Instant start;
    public Instant end;

    public String epg;
    @BsonProperty("channel_name")
    public String channel;
    public Integer duration;
    public String genre;
    public String image;
    public String type;
    public Integer level;
    @BsonProperty("_drm")
    public DrmConfig drm;
}

