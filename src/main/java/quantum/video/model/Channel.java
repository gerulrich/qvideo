package quantum.video.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@MongoEntity(collection="channels")
public class Channel {
    public ObjectId id;
    public String name;
    public Integer number;
    public String code;
    public String url;
    public String logo;
    public String category;
    public String plan;
    public Integer level;
    public String egp;
    public boolean enabled;
    public String epg;
    public boolean proxy;
    @BsonProperty("_drm")
    public DrmConfig drm;
}

