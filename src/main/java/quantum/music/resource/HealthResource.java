package quantum.music.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class HealthResource {

    @GET
    @Path("/health")
    public String health() {
        return "OK";
    }

}
