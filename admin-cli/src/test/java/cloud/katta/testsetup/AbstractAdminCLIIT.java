/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.junit.jupiter.api.BeforeEach;

import cloud.katta.client.ApiClient;
import cloud.katta.client.auth.HttpBearerAuth;

import static io.restassured.RestAssured.given;

public class AbstractAdminCLIIT {
    protected String accessToken;
    protected ApiClient apiClient;

    @BeforeEach
    protected void setup() throws Exception {
        accessToken = given()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .formParam("client_id", "cryptomator")
                .formParam("grant_type", "password")
                .formParam("username", "admin")
                .formParam("password", "admin")
                .when()
                .post("http://localhost:8380/realms/cryptomator/protocol/openid-connect/token")
                .then()
                .statusCode(200)
                .extract().path("access_token");
        final HttpBearerAuth auth = new HttpBearerAuth("Bearer");
        apiClient = new ApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer " + accessToken);
        apiClient.setBasePath("http://localhost:8280");
    }
}
