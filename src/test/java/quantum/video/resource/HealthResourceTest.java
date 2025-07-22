package quantum.video.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
public class HealthResourceTest {

    @Test
    public void testHealthCheck() {
        given()
          .when().get("/health")
          .then()
             .statusCode(200)
             .body(is("OK"));
    }

    @Test
    public void testVersionEndpoint() {
        given()
          .when().get("/version")
          .then()
            .statusCode(200)
            .body(is("test"));
    }

}