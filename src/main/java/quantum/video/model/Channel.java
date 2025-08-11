package quantum.video.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

/**
 * Represents a streaming channel in the system.
 * <p>
 * This entity class maps to the "channels" collection in MongoDB and contains
 * all the information related to a broadcast channel, including its metadata,
 * access control settings, and streaming configuration.
 * </p>
 * <p>
 * Channels are central entities in the video streaming system and are referenced
 * by programs, manifests, and other components throughout the application.
 * </p>
 */
@MongoEntity(collection="channels")
public class Channel {
    /**
     * Unique identifier for the channel in MongoDB.
     */
    public ObjectId id;

    /**
     * The display name of the channel.
     */
    public String name;

    /**
     * The channel number in the EPG/lineup.
     */
    public Integer number;

    /**
     * The unique code identifier for the channel, used in URLs and API paths.
     */
    public String code;

    /**
     * The source URL for the channel's streaming content.
     */
    public String url;

    /**
     * URL or path to the channel's logo image.
     */
    public String logo;

    /**
     * The category or genre the channel belongs to (e.g., sports, news, entertainment).
     */
    public String category;

    /**
     * The subscription plan level this channel belongs to.
     */
    public String plan;

    /**
     * The authorization level required to access this channel (lower numbers mean wider access).
     */
    public Integer level;

    /**
     * Electronic Program Guide reference identifier.
     */
    public String egp;

    /**
     * Flag indicating whether the channel is currently enabled for viewing.
     */
    public boolean enabled;

    /**
     * Electronic Program Guide identifier.
     */
    public String epg;

    /**
     * Flag indicating whether the channel's content should be proxied through the system.
     */
    public boolean proxy;

    /**
     * Digital Rights Management configuration for the channel, if applicable.
     * Mapped from the "_drm" field in MongoDB.
     */
    @BsonProperty("_drm")
    public DrmConfig drm;
}
