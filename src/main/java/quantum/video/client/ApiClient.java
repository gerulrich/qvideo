package quantum.video.client;

import io.quarkus.rest.client.reactive.ClientQueryParam;
import io.vertx.core.json.JsonArray;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * REST client interface for interacting with the Flow API.
 * Provides methods to search for channel content with specific filtering parameters.
 * Uses custom headers provided by {@link ApiHeadersFactory}.
 */
@Path("/api/v1")
@RegisterRestClient(configKey = "flow-api")
@RegisterClientHeaders(ApiHeadersFactory.class)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ApiClient {

    /**
     * Searches for channel content based on date range and channel list.
     * Results are returned with a maximum of 1440 items, filtered by TV rating 6,
     * and include all available content types.
     *
     * @param dateFrom The start date for the content search in ISO format
     * @param dateTo The end date for the content search in ISO format
     * @param channels List of channel IDs to filter the search results
     * @return A Uni containing a JsonArray of matching channel content items
     */
    @POST
    @Path("/content/channel")
    @ClientQueryParam(name = "size", value = "1440")
    @ClientQueryParam(name = "tvRating", value = "6")
    @ClientQueryParam(name = "all", value = "true")
    Uni<JsonArray> search(
            @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo,
            List<Integer> channels
    );
}