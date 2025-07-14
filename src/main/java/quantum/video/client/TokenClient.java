package quantum.video.client;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * TokenClient is a REST client interface for interacting with the authentication SDK API.
 * It provides methods to obtain tokens using the provided request and authorization header.
 */
@RegisterRestClient(configKey = "auth-sdk-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "User-Agent", value = "Ktor client")
public interface TokenClient {

    /**
     * Sends a POST request to the authentication SDK API to retrieve a token.
     *
     * @param request the request payload containing necessary data for token generation
     * @param token the authorization token to be included in the request header
     * @return a Uni containing the JSON response from the API
     */
    @POST
    @Path("/auth-sdk/v1/mamushka")
    Uni<JsonObject> token(MamushkaRequest request, @HeaderParam("Authorization") String token);

}
