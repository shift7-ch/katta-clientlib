/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup;

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

import static ch.iterate.hub.testsetup.AbstractHubTest.UNATTENDED_LOCAL_KEYCLOAK_TESTING;
import static ch.iterate.hub.testsetup.AbstractHubTest.UNATTENDED_LOCAL_ONLY;

public abstract class HubTestSetupDockerExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger log = LogManager.getLogger(HubTestSetupDockerExtension.class.getName());

    private ComposeContainer compose;

    protected void setupDocker(final HubTestConfig.Setup.DockerConfig configuration) throws URISyntaxException {
        log.info("Setup docker {}", configuration);
        this.compose = new ComposeContainer(
                new File(HubTestSetupDockerExtension.class.getResource(configuration.composeFile).toURI()))
                .withLocalCompose(true)
                .withPull(true)
                .withEnv(
                        Stream.of(
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTP_PORT", Integer.toString(configuration.keycloakServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTPS_PORT", "8443"),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_PORT", Integer.toString(configuration.minioServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_CONSOLE_PORT", Integer.toString(configuration.minioConsolePort)),
                                new AbstractMap.SimpleImmutableEntry<>("HUB_PORT", Integer.toString(configuration.hubPort))
                        ).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)))
                .withLogConsumer("minio-1", outputFrame -> log.debug("[minio_1] {}", outputFrame.getUtf8String()))
                .withExposedService("minio-1", configuration.minioServicePort, Wait.forListeningPort())
                .withExposedService("keycloak-1", configuration.keycloakServicePort, Wait.forListeningPort())
                .withExposedService("hub-1", configuration.hubPort, Wait.forListeningPort())
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)))
                .waitingFor("hub_setup_storage_profile-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();
        log.info("Done setup docker {}", configuration);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        log.info("Stop docker {}", compose);
        compose.stop();
    }

    public static class UnattendedLocalOnly extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
            this.setupDocker(UNATTENDED_LOCAL_ONLY.dockerConfig);
        }
    }

    public static class UnattendedLocalKeycloakDev extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException {
            this.setupDocker(UNATTENDED_LOCAL_KEYCLOAK_TESTING.dockerConfig);
        }
    }
}
