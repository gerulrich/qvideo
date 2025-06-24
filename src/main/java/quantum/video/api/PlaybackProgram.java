package quantum.video.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bson.types.ObjectId;
import quantum.video.model.Program;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static java.lang.String.format;

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

        public PlaybackProgram(Program program, boolean includeDrm) {
            this(
                program.id,
                program.title,
                program.description,
                program.start.atZone(ZoneId.systemDefault()),
                program.end.atZone(ZoneId.systemDefault()),
                program.url != null ? format(
                        "http://localhost:8080/pvr/manifest/%s/%s",
                        program.id,
                        program.url.substring(program.url.lastIndexOf('/') + 1)
                ): "",
                program.episode,
                program.genre,
                includeDrm && program.drm != null ? new DrmInfo(program.drm) : null
            );
        }
}
