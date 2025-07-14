package quantum.video.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import quantum.video.service.TokenService;

/**
 * Factory for creating and configuring HTTP headers for API client requests.
 * This class automatically adds authentication via Bearer token and other required headers
 * to all outgoing requests from ApiClient instances.
 * <p>
 * The token is retrieved from the injected TokenService which manages authentication
 * token caching and retrieval.
 */
@ApplicationScoped
public class ApiHeadersFactory implements ClientHeadersFactory {

    @Inject
    TokenService tokenService;

    private static final Logger LOG = Logger.getLogger(ApiHeadersFactory.class);

    /**
     * Updates the outgoing request headers with authentication and content type information.
     * This method is called automatically by the REST client framework for each request.
     *
     * @param incomingHeaders The headers from the incoming JAX-RS request, may be null
     * @param clientOutgoingHeaders The headers configured on the outgoing client request
     * @return A MultivaluedMap containing all the headers to be sent with the request
     */
    @Override
    public MultivaluedMap<String, String> update(
            MultivaluedMap<String, String> incomingHeaders,
            MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        MultivaluedMap<String, String> result = new MultivaluedHashMap<>();
        result.add("Authorization", String.format("Bearer %s", tokenService.getCachedToken()));
        result.add("Content-Type", "application/json");
        result.add("User-Agent", "okhttp/4.11.0");
        LOG.infof("Headers configured: %s", result.keySet());
        return result;
    }
}