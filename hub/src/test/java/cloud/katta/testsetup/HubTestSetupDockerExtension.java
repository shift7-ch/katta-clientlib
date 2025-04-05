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
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    protected void setupDocker(final HubTestConfig.Setup.DockerConfig configuration) throws URISyntaxException {
        log.info("Setup docker {}", configuration);
        this.compose = new ComposeContainer(
                new File(HubTestSetupDockerExtension.class.getResource(configuration.composeFile).toURI()))
                .withLocalCompose(true)
                .withPull(true)
                .withEnv(
                        Stream.of(
                                new AbstractMap.SimpleImmutableEntry<>("HUB_PORT", Integer.toString(configuration.hubPort)),
                                new AbstractMap.SimpleImmutableEntry<>("HUB_KEYCLOAK_URL", configuration.hubKeycloakUrl),
                                new AbstractMap.SimpleImmutableEntry<>("HUB_KEYCLOAK_REALM", configuration.hubKeycloakRealm),

                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HOSTNAME", "localhost"),
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTP_PORT", Integer.toString(configuration.keycloakServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTPS_PORT", "8443"),

                                new AbstractMap.SimpleImmutableEntry<>("MINIO_HOSTNAME", "localhost"),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_PORT", Integer.toString(configuration.minioServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_CONSOLE_PORT", Integer.toString(configuration.minioConsolePort))

                        ).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)))
                .withOptions(configuration.profile == null ? "" : String.format("--profile=%s", configuration.profile))
                .withLogConsumer("minio-1", outputFrame -> log.debug("[minio_1] {}", outputFrame.getUtf8String()))
                .withExposedService("minio-1", configuration.minioServicePort, Wait.forListeningPort())
                .withExposedService("keycloak-1", configuration.keycloakServicePort, Wait.forListeningPort())
                .withExposedService("hub-1", configuration.hubPort, Wait.forListeningPort())
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)))
                .waitingFor("hub_setup_storage_profile-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();
        log.info("Done setup docker {}", configuration);
    }

    /**
     * Local
     */
    public static class Local extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
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
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
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
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
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
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
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
