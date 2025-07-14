package quantum.video.client;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * SessionClient is a REST client interface for interacting with the authentication flow API.
 * It provides methods to create and manage user sessions with the service.
 */
@RegisterRestClient(configKey = "auth-flow-api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ClientHeaderParam(name = "User-Agent", value = "Ktor client")
public interface SessionClient {

    /**
     * Creates or retrieves a session for the specified account.
     *
     * @param account the account identifier for which to create a session
     * @param request the request payload containing session initialization data
     * @return a Uni containing the Response from the API which includes session details
     */
    @POST
    @Path("/xtv-ws-client/api/v1/session/{account}")
    Uni<Response> session(@PathParam("account") String account, SessionRequest request);

}
