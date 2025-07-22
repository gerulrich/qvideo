package quantum.video.service;

import jakarta.enterprise.context.ApplicationScoped;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import quantum.video.client.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Service for managing authentication tokens for external API interactions.
 * <p>
 * This service handles token acquisition, caching, and renewal for accessing
 * external streaming services. It manages the lifecycle of authentication tokens,
 * including automatic renewal when tokens expire, and provides methods for executing
 * operations that require valid authentication.
 * </p>
 * <p>
 * The service interacts with external authentication endpoints via REST clients
 * and follows a reactive programming model using Mutiny for non-blocking operations.
 * It implements token caching with expiration tracking to minimize authentication
 * requests.
 * </p>
 * <p>
 * Configuration for this service is supplied via MicroProfile Config properties
 * that specify the necessary credentials and device information for authentication.
 * </p>
 */
@ApplicationScoped
public class TokenService {

    @ConfigProperty(name = "flow.access.token")
    private String flowAccessToken;

    @ConfigProperty(name = "flow.device.token")
    private String deviceToken;

    @ConfigProperty(name = "flow.cas.id")
    private String casId;

    @ConfigProperty(name = "flow.mac")
    private String mac;

    @ConfigProperty(name = "flow.device.uuid")
    private String deviceUuid;

    @ConfigProperty(name = "flow.device.app.version")
    private String deviceAppVersion;

    @ConfigProperty(name = "flow.account")
    private String account;

    private static final String SPACE = " ";
    private static final String AUTHORIZATION = "Authorization";

    @Inject
    @RestClient
    SessionClient sessionClient;

    @Inject
    @RestClient
    TokenClient tokenClient;

    private String currentToken;
    private Instant expiresAt;

    /**
     * Retrieves the authorization token for API operations.
     * <p>
     * This method returns the current valid token if one exists and has not expired.
     * If no token exists or the current token has expired, this method automatically
     * initiates the token renewal process to obtain a fresh token.
     * </p>
     * <p>
     * The token retrieval process follows a reactive approach, returning a {@link Uni}
     * that completes when a valid token is available.
     * </p>
     *
     * @return A {@link Uni} containing the valid access token for API authorization
     */
    public Uni<String> getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            return renewToken();
        }
        return Uni.createFrom().item(currentToken);
    }

    /**
     * Returns the cached token without checking its validity or initiating renewal.
     * <p>
     * This method provides direct access to the currently cached token without
     * performing expiration checks or triggering renewal processes. It may return
     * null if no token has been acquired yet or if the token has been explicitly
     * cleared.
     * </p>
     *
     * @return The currently cached token, or null if no token is available
     */
    public String getCachedToken() {
        return currentToken;
    }

    /**
     * Executes an operation that requires an authentication token.
     * <p>
     * This method ensures that a valid token is available before executing the
     * supplied function. It handles token acquisition or renewal transparently,
     * simplifying operations that require authentication.
     * </p>
     * <p>
     * The operation is executed in a reactive context, with proper error handling
     * and non-blocking behavior throughout the token acquisition and operation
     * execution flow.
     * </p>
     *
     * @param <R> The type of result produced by the operation
     * @param fn A supplier function that executes the operation and returns a {@link Uni} with the result
     * @return A {@link Uni} containing the result of the operation
     */
    public <R> Uni<R> withToken(Supplier<Uni<R>> fn) {
        return getToken().onItem().transformToUni(token -> fn.get());
    }

    /**
     * Renews the authentication token by making requests to the authentication services.
     * <p>
     * This private method implements the token renewal process, which involves:
     * <ol>
     *   <li>Obtaining a session from the session service</li>
     *   <li>Using that session to request a new token from the token service</li>
     *   <li>Caching the new token and setting its expiration time</li>
     * </ol>
     * </p>
     * <p>
     * The method includes retry logic with exponential backoff to handle transient
     * network or service failures during the renewal process.
     * </p>
     *
     * @return A {@link Uni} containing the newly acquired token
     */
    private Uni<String> renewToken() {
        return this.getSession()
                .flatMap(session -> tokenClient.token(mamushkaRequest(session), "Bearer " + flowAccessToken)
                .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10)).atMost(3)
                .onItem().transform(json -> {
                    this.currentToken = json.getString("token_mamushka");
                    this.expiresAt = Instant.now().plusSeconds(86340);
                    return this.currentToken;
                }));
    }

    /**
     * Obtains a session token from the session service.
     * <p>
     * This private helper method initiates a session with the authentication service
     * using account and device information. It includes retry logic to handle
     * transient failures during the session acquisition process.
     * </p>
     *
     * @return A {@link Uni} containing the session token extracted from the response
     */
    private Uni<String> getSession() {
        return sessionClient.session(account, new SessionRequest(device(), deviceToken))
            .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10)).atMost(3)
            .onItem().transform(response -> response.getHeaderString(AUTHORIZATION).split(SPACE)[1]);
    }

    /**
     * Creates a device information object with the configured device parameters.
     * <p>
     * This private helper method encapsulates the process of building a device
     * information object from the service's configuration properties.
     * </p>
     *
     * @return A {@link DeviceInfo} object containing the configured device parameters
     */
    private DeviceInfo device() {
        return new DeviceInfo(deviceAppVersion, casId, mac, deviceAppVersion, deviceUuid);
    }

    /**
     * Creates a request object for token renewal using the provided session.
     * <p>
     * This private helper method constructs a properly formatted request object
     * that includes the session token and device information required for
     * token renewal.
     * </p>
     *
     * @param session The session token to include in the request
     * @return A {@link MamushkaRequest} object containing the necessary authentication parameters
     */
    private MamushkaRequest mamushkaRequest(String session) {
        return new MamushkaRequest(session, deviceToken, casId, device());
    }
}
