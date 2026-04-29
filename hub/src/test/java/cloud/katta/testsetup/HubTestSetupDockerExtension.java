/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * Profiles:
 * - local: Hub, Keycloak, MinIO in testcontainers
 * - hybrid-<i>stage</i>: Hub in testcontainers, use existing Keycloak and MinIO
 * - remote-<i>stage</i>: use existing Hub, Keycloak and MinIO
 * - remote-deployment-<i>stage</i>: deploy Hub and Keycloak
 * <p>
 * For each level <i>L</i>, 3 modes:
 * - <i>L</i>: run setup, tests and teardown
 * - <i>L</i>KeepRunning: run setup and tests, skip teardown
 * - <i>L</i>AlreadyRunning: run tests, skip setup and teardown
 */
public abstract class HubTestSetupDockerExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger log = LogManager.getLogger(HubTestSetupDockerExtension.class.getName());

    private static final long FOREVER = Long.MAX_VALUE;

    protected ComposeContainer compose;

    protected void setupDocker(final HubTestConfig.Setup.DockerConfig dockerConfig) throws URISyntaxException, IOException {
        log.info("Setup docker {}", dockerConfig);
        final Properties configuration = new Properties();
        try (InputStream in = Objects.requireNonNull(this.getClass().getResourceAsStream(dockerConfig.envFile))) {
            configuration.load(in);
        }
        final HashMap<String, String> env = configuration.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new
                ));
        compose = new ComposeContainer(
                new File(HubTestSetupDockerExtension.class.getResource(dockerConfig.composeFile).toURI()))
                .withPull(true)
                .withEnv(env)
                .withOptions(dockerConfig.profile == null ? "" : String.format("--profile=%s", dockerConfig.profile))
                .waitingFor("hub", new LogMessageWaitStrategy().withRegEx(".*Completed Hub Setup.*").withStartupTimeout(Duration.ofMinutes(2)));
        if("local".equals(dockerConfig.profile)) {
            compose.waitingFor("minio_setup", new LogMessageWaitStrategy().withRegEx(".*Completed MinIO Setup.*").withStartupTimeout(Duration.ofMinutes(2)));
        }
        compose.start();
        log.info("Done setup docker {}", dockerConfig);
    }

    /**
     * Local
     */
    public static class Local extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException, IOException {
            this.setupDocker(AbstractHubTest.LOCAL_DOCKER_CONFIG);
        }

        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            log.info("Stop docker {}", this.compose);
            this.compose.stop();
        }
    }

    public static class LocalKeepRunning extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException, IOException {
            this.setupDocker(AbstractHubTest.LOCAL_DOCKER_CONFIG);
        }

        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            log.info("Tests done, keep running {}", this.compose);
            Thread.sleep(FOREVER);
        }
    }

    public static class LocalAlreadyRunning extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
            // no setup
        }

        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            // no teardown
        }
    }

    /**
     * Hybrid
     */
    public static class HybridTesting extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException, IOException {
            this.setupDocker(AbstractHubTest.HYBRID_DOCKER_CONFIG);
        }

        @Override
        public void afterAll(final ExtensionContext context) {
            log.info("Stop docker {}", compose);
            compose.stop();
        }
    }

    public static class HybridTestingKeepRunning extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException, IOException {
            this.setupDocker(AbstractHubTest.HYBRID_DOCKER_CONFIG);
        }

        @Override
        public void afterAll(final ExtensionContext context) throws Exception {
            log.info("Tests done, keep running {}", this.compose);
            Thread.sleep(FOREVER);
        }
    }

    public static class HybridTestingAlreadyRunning extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) {
            // no setup
        }

        @Override
        public void afterAll(final ExtensionContext context) {
            // no teardown
        }
    }
}
