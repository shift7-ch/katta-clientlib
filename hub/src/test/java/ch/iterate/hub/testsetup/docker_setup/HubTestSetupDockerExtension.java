/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.docker_setup;

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

import ch.iterate.hub.testsetup.model.HubTestSetupConfig;

public abstract class HubTestSetupDockerExtension implements BeforeAllCallback, AfterAllCallback {
    private static final Logger log = LogManager.getLogger(HubTestSetupDockerExtension.class.getName());

    private ComposeContainer compose;

    protected void setupDocker(final HubTestSetupConfig.DockerConfig configuration) throws URISyntaxException {
        log.info(String.format("Setup docker %s", configuration));
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
                .withLogConsumer("minio-1", outputFrame -> log.debug("[minio_1] " + outputFrame.getUtf8String()))
                .withExposedService("minio-1", configuration.minioServicePort, Wait.forListeningPort())
                .withExposedService("keycloak-1", configuration.keycloakServicePort, Wait.forListeningPort())
                .withExposedService("hub-1", configuration.hubPort, Wait.forListeningPort())
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)))
                .waitingFor("hub_setup_storage_profile-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();
        log.info(String.format("Done setup docker %s", configuration));
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        log.info(String.format("Stop docker %s", compose));
        compose.stop();
    }
}
