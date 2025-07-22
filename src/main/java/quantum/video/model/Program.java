package quantum.video.model;

import io.quarkus.mongodb.panache.common.MongoEntity;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

import java.time.Instant;

/**
 * Represents a television program or recorded content in the system.
 * <p>
 * This entity class maps to the "programs" collection in MongoDB and contains all the
 * information related to a program, including its metadata, timing information, and
 * content references. Programs are associated with channels and can be accessed through
 * the Personal Video Recording (PVR) subsystem.
 * </p>
 * <p>
 * The class includes fields for both live and on-demand playback, with scheduling
 * information (start/end times) and references to streaming resources.
 * </p>
 */
@MongoEntity(collection="programs")
public class Program {
    /**
     * Unique identifier for the program in MongoDB.
     */
    public ObjectId id;

    /**
     * The title of the program.
     */
    public String title;

    /**
     * Detailed description of the program content.
     */
    public String description;

    /**
     * Title of the specific episode if this program is part of a series.
     * Mapped from the "episode_title" field in MongoDB.
     */
    @BsonProperty("episode_title")
    public String episode;

    /**
     * The source URL for the program's streaming content.
     */
    public String url;

    /**
     * The scheduled start time of the program broadcast.
     */
    public Instant start;

    /**
     * The scheduled end time of the program broadcast.
     */
    public Instant end;

    /**
     * Electronic Program Guide reference identifier.
     */
    public String epg;

    /**
     * The channel associated with this program.
     * Mapped from the "channel_name" field in MongoDB.
     */
    @BsonProperty("channel_name")
    public String channel;

    /**
     * Duration of the program in seconds.
     */
    public Integer duration;

    /**
     * The genre or category of the program (e.g., movie, sports, news).
     */
    public String genre;

    /**
     * URL or path to the program's thumbnail or cover image.
     */
    public String image;

    /**
     * Classification of the program content type (e.g., movie, series, documentary).
     */
    public String type;

    /**
     * The authorization level required to access this program (lower numbers mean wider access).
     */
    public Integer level;

    /**
     * Digital Rights Management configuration for the program, if applicable.
     * Mapped from the "_drm" field in MongoDB.
     */
    @BsonProperty("_drm")
    public DrmConfig drm;
}
