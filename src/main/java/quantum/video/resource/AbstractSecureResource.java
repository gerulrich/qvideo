package quantum.video.resource;

import io.smallrye.jwt.auth.principal.JWTCallerPrincipal;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

/**
 * Abstract base class for secured REST resources.
 * <p>
 * This class provides common functionality for resources that require
 * user authentication and authorization level checks. It extracts
 * user level information from the security context for authorization decisions.
 * </p>
 */
public abstract class AbstractSecureResource {

    /**
     * Extracts the user authorization level from the security context.
     * <p>
     * This method determines the user's access level from their JWT token claims.
     * If no user is authenticated or no level claim is found, it defaults to level 0.
     * </p>
     *
     * @param ctx The security context containing user principal information
     * @return The user's authorization level as an integer
     */
    protected int getUserLevel(SecurityContext ctx) {
        Principal principal = ctx.getUserPrincipal();
        return switch (principal) {
            case JWTCallerPrincipal jwt ->
                    jwt.claim("level")
                            .map(v -> Integer.parseInt(v.toString()))
                            .orElse(0);
            case null, default -> 0;
        };
    }
}
