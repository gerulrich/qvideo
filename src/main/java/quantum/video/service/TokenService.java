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
     * Retrieves the authorization token. If the token is expired or not present, it will renew it.
     *
     * @return A Uni containing the access token.
     */
    public Uni<String> getToken() {
        if (currentToken == null || Instant.now().isAfter(expiresAt)) {
            return renewToken();
        }
        return Uni.createFrom().item(currentToken);
    }

    /**
     * Returns the cached token if it is still valid.
     *
     * @return The cached token or null if it has expired.
     */
    public String getCachedToken() {
        return currentToken;
    }

    /**
     * Executes a function that requires the token. The function will be called with the token.
     *
     * @param fn A function that takes a token and returns a Uni.
     * @param <R> The type of the result.
     * @return A Uni containing the result of the function.
     */
    public <R> Uni<R> withToken(Supplier<Uni<R>> fn) {
        return getToken().onItem().transformToUni(token -> fn.get());
    }

    /**
     * Renews the authorization token by making a request to the token client.
     *
     * @return A Uni containing the renewed token.
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

    private Uni<String> getSession() {
        return sessionClient.session(account, new SessionRequest(device(), deviceToken))
            .onFailure().retry().withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10)).atMost(3)
            .onItem().transform(response -> response.getHeaderString(AUTHORIZATION).split(SPACE)[1]);
    }

    private DeviceInfo device() {
        return new DeviceInfo(deviceAppVersion, casId, mac, deviceAppVersion, deviceUuid);
    }

    private MamushkaRequest mamushkaRequest(String session) {
        return new MamushkaRequest(session, deviceToken, casId, device());
    }
}

