/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testcontainers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.Pair;
import cloud.katta.client.auth.HttpBearerAuth;

import static io.restassured.RestAssured.given;

public class AbtractAdminCliIT {
    private static final Logger log = LogManager.getLogger(AbtractAdminCliIT.class.getName());
    public static ComposeContainer compose;

    protected String accessToken;
    protected ApiClient apiClient;

    @BeforeAll
    public static void setupDocker() throws URISyntaxException, IOException {
        final String composeFile = "/docker-compose-minio-localhost-hub.yml";
        final String envFile = "/.local.env";
        final String profile = "local";
        final String hubAdminUser = "admin";
        final String hubAdminPassword = "admin";
        final String hubKeycloakSystemClientSecret = "top-secret";
        final Properties props = new Properties();
        props.load(AbtractAdminCliIT.class.getResourceAsStream(envFile));
        final HashMap<String, String> env = props.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new
                ));
        env.put("HUB_ADMIN_USER", hubAdminUser);
        env.put("HUB_ADMIN_PASSWORD", hubAdminPassword);
        env.put("HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET", hubKeycloakSystemClientSecret);
        compose = new ComposeContainer(
                new File(AbtractAdminCliIT.class.getResource(composeFile).toURI()))
                .withLocalCompose(true)
                .withPull(true)
                .withEnv(env)
                .withOptions(profile == null ? "" : String.format("--profile=%s", profile))
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*Completed MinIO Setup.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();
    }

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

    @AfterAll
    public static void teardownDocker() {
        try {
            compose.stop();
        }
        catch(Exception e) {
            log.warn(e);
        }
    }
}
