package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.bson.types.ObjectId;

import java.time.ZonedDateTime;

/**
 * Data Transfer Object representing the currently playing program on a channel.
 * <p>
 * Contains metadata and timing information for the program that is live or on air.
 * </p>
 *
 * @param id          Unique identifier of the program
 * @param title       Title of the program
 * @param description Description of the program
 * @param startTime   Start time of the program (ZonedDateTime, ISO 8601)
 * @param endTime     End time of the program (ZonedDateTime, ISO 8601)
 */
public record NowPlaying(
        ObjectId id,
        String title,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
        ZonedDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
        ZonedDateTime endTime
) {
}
