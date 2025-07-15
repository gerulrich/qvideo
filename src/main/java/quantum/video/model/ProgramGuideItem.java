package quantum.video.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Domain record representing an Electronic Program Guide (EPG) item.
 * Contains program schedule information including timing, content metadata,
 * and resource links for a specific TV program.
 * <p>
 * This class is immutable and thread-safe.
 *
 * @param id ID of the program
 * @param title Title of the program
 * @param description Full description of the program content
 * @param episode Episode title or identifier if the program is part of a series
 * @param url URL to the program's streaming resource
 * @param image URL to the program's cover image
 * @param genre Genre classification of the program
 * @param duration Duration of the program in minutes
 * @param start Start time of the program as Unix timestamp (milliseconds)
 * @param end End time of the program as Unix timestamp (milliseconds)
 */
public record ProgramGuideItem(
        String id,
        String title,
        String description,
        String episode,
        String url,
        String image,
        String genre,
        Integer duration,
        Long start,
        Long end
) {

    /**
     * Constructs a ProgramGuideItem from a JsonObject representation.
     * Extracts and maps all relevant fields from the JSON structure.
     *
     * @param item JsonObject containing program guide data
     */
    public ProgramGuideItem(JsonObject item) {
        this(
                item.getString("id"),
                item.getString("title"),
                item.getString("description"),
                item.getString("episodeTitle"),
                getUrl(item.getJsonArray("resources")),
                getImage(item.getJsonArray("images")),
                item.getString("genre"),
                item.getInteger("duration"),
                item.getLong("startTime"),
                item.getLong("endTime")
        );
    }

    /**
     * Extracts the appropriate streaming URL from resources array.
     * Prioritizes DASH protocol with Widevine encryption.
     *
     * @param resources JsonArray containing available streaming resources
     * @return The URL string or null if no matching resource is found
     */
    private static String getUrl(JsonArray resources) {
        return resources.isEmpty() ? null : resources
                .stream()
                .map(JsonObject.class::cast)
                .filter(resource -> "DASH".equals(resource.getString("protocol")) && "Widevine".equals(resource.getString("encryption")))
                .map(resource -> resource.getString("url"))
                .toList().getFirst();
    }

    /**
     * Extracts the appropriate image URL from images array.
     * Prioritizes images marked for BROWSE usage.
     *
     * @param images JsonArray containing available images
     * @return The formatted image URL string or null if no matching image is found
     */
    private static String getImage(JsonArray images) {
        return images.isEmpty() ? null : images
                .stream()
                .map(JsonObject.class::cast)
                .filter(image -> "BROWSE".equals(image.getString("usage")))
                .map(image -> image.getString("suffix") + "." + image.getString("format"))
                .toList().getFirst();
    }
}
