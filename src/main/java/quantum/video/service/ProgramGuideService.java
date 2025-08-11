package quantum.video.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import quantum.video.client.ApiClient;
import quantum.video.model.ProgramGuideItem;

import java.time.Duration;
import java.util.List;

/**
 * Service responsible for retrieving electronic program guide (EPG) data.
 * Provides methods to fetch program guide information for specified channels and date ranges.
 * Communicates with external API through the ApiClient and transforms JSON responses into domain objects.
 */
@ApplicationScoped
public class ProgramGuideService {

    private static final Logger LOG = Logger.getLogger(ProgramGuideService.class);

    @Inject
    @RestClient
    ApiClient apiClient;

    /**
     * Retrieves program guide items for specified channels and date range.
     * Implements retry logic with exponential backoff for resilience against temporary failures.
     *
     * @param dateFrom Starting date for the program guide data in ISO format
     * @param dateTo Ending date for the program guide data in ISO format
     * @param channels List of channel IDs to fetch program guide for
     * @return A Uni emitting a List of ProgramGuideItem objects
     */
    public Uni<List<ProgramGuideItem>> getProgramGuide(String dateFrom, String dateTo, List<Integer> channels) {
        LOG.infof("Retrieving program guide data from %s to %s for channels: %s", dateFrom, dateTo, channels);
        return apiClient.search(dateFrom, dateTo, channels)
                .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10)).atMost(3)
                .onFailure().invoke(throwable ->
                        LOG.errorf(throwable, "Failed to retrieve program guide data after retries for channels: %s", channels)
                )
                .onItem().invoke(() -> LOG.infof("Program guide data successfully retrieved for channels: %s", channels))
                .onItem().transform(json -> {
                    JsonArray items = json.getJsonArray(0);
                    return items.stream()
                        .map(JsonObject.class::cast)
                        .map(ProgramGuideItem::new).toList();
            });
    }
}
