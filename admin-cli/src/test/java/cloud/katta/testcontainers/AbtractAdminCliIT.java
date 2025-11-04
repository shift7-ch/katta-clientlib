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
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;

public class AbtractAdminCliIT extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbtractAdminCliIT.class.getName());
    public static ComposeContainer compose;

    protected HubSession hubSession;

    @BeforeAll
    public static void setupDocker() throws URISyntaxException, IOException {
        final HubTestConfig.Setup.DockerConfig configuration = new HubTestConfig.Setup.DockerConfig("/docker-compose-minio-localhost-hub.yml", "/.local.env", "local", "admin", "admin", "top-secret");
        log.info("Setup docker {}", configuration);
        final Properties props = new Properties();
        props.load(AbtractAdminCliIT.class.getResourceAsStream(configuration.envFile));
        final HashMap<String, String> env = props.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new
                ));
        env.put("HUB_ADMIN_USER", configuration.hubAdminUser);
        env.put("HUB_ADMIN_PASSWORD", configuration.hubAdminPassword);
        env.put("HUB_KEYCLOAK_SYSTEM_CLIENT_SECRET", configuration.hubKeycloakSystemClientSecret);
        compose = new ComposeContainer(
                new File(AbtractAdminCliIT.class.getResource(configuration.composeFile).toURI()))
                .withLocalCompose(true)
                .withPull(true)
                .withEnv(env)
                .withOptions(configuration.profile == null ? "" : String.format("--profile=%s", configuration.profile))
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*Completed MinIO Setup.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();

        log.info("Done setup docker {}", configuration);
    }

    @BeforeEach
    protected void setup() throws Exception {
        final HubTestConfig.Setup config = new HubTestConfig.Setup().withUserConfig(
                new HubTestConfig.Setup.UserConfig("admin", "admin", "setupcode")
        ).withHubURL("http://localhost:8280");
        hubSession = setupConnection(config);
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
