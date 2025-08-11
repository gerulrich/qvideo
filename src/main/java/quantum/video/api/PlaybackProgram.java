package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bson.types.ObjectId;
import quantum.video.model.Program;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;

/**
 * Data Transfer Object representing a program available for playback.
 * <p>
 * This record is used as an API response for program playback information, including
 * metadata, timing, streaming URL, and DRM details.
 * </p>
 *
 * @param id         Unique identifier of the program
 * @param title      Title of the program
 * @param description Description of the program
 * @param startTime  Start time of the program (ZonedDateTime, ISO 8601)
 * @param endTime    End time of the program (ZonedDateTime, ISO 8601)
 * @param url        Streaming URL for playback
 * @param episode    Episode information, if applicable
 * @param genre      Genre of the program
 * @param drm        DRM information for protected content
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlaybackProgram(
        ObjectId id,
        String title,
        String description,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
        ZonedDateTime startTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssZ")
        ZonedDateTime endTime,
        String url,
        String episode,
        String genre,
        DrmInfo  drm
        ) {

        public PlaybackProgram(Program program) {
            this(program, null, false);
        }

        public PlaybackProgram(Program program, String patternUrl, boolean includeDrm) {
            this(
                program.id,
                program.title,
                program.description,
                program.start.atZone(ZoneId.systemDefault()),
                program.end.atZone(ZoneId.systemDefault()),
                patternUrl != null && program.url != null ? format(
                        patternUrl,
                        program.id,
                        program.url.substring(program.url.lastIndexOf('/') + 1)
                ): "",
                program.episode,
                program.genre,
                includeDrm && program.drm != null ? new DrmInfo(program.drm) : null
            );
        }
}
