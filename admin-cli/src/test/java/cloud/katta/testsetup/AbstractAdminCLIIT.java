/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.util.List;
import java.util.Map;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.Pair;
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
        auth.setBearerToken(accessToken);
        apiClient = new ApiClient() {
            protected void updateParamsForAuth(String[] authNames, List<Pair> queryParams, Map<String, String> headerParams,
                                               Map<String, String> cookieParams, String payload, String method, URI uri) throws ApiException {
                auth.applyToParams(queryParams, headerParams, cookieParams, payload, method, uri);
            }
        };
        apiClient.setBasePath("http://localhost:8280");
    }
}
