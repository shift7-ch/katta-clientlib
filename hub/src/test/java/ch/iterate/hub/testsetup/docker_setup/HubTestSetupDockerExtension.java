/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.docker_setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.iterate.hub.testsetup.model.HubTestSetupConfig;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.AuthConfig;

public abstract class HubTestSetupDockerExtension implements BeforeAllCallback {
    private static final Logger log = LogManager.getLogger(HubTestSetupDockerExtension.class.getName());

    // TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 use release version! can we extract to pom.xml?
    final static String CIPHERDUCK_HUB_VERSION = "latest";
    final static String CIPHERDUCK_KEYCLOAK_VERSION = "25.0.1";

    private ComposeContainer compose;

    protected void setupDocker(final HubTestSetupConfig.DockerConfig dockerConfig) throws URISyntaxException, IOException {
        log.info(String.format("Setup docker %s", dockerConfig));
        final String composeFile = dockerConfig.composeFile;
        final int keycloakServicePort = dockerConfig.keycloakServicePort;
        final int minioServicePort = dockerConfig.minioServicePort;
        final int minioConsolePort = dockerConfig.minioConsolePort;
        final int hubPort = dockerConfig.hubPort;


        final List<String> images = Arrays.asList(
                String.format("ghcr.io/shift7-ch/katta-server:%s", CIPHERDUCK_HUB_VERSION),
                String.format("ghcr.io/shift7-ch/keycloak:%s", CIPHERDUCK_KEYCLOAK_VERSION)
        );
        for(final String image : images) {
            log.info(String.format("pulling %s...", image));
            DockerClientFactory.instance().client().pullImageCmd(image)
                    .withAuthConfig(new AuthConfig().withRegistryAddress("ghcr.io"))
                    .exec(new PullImageResultCallback()).onComplete();
            log.info(String.format("pulled %s...", image));
        }

        log.info(String.format("PATH=%s", System.getenv("PATH")));


        this.compose = new ComposeContainer(
                new File(HubTestSetupDockerExtension.class.getResource(composeFile).toURI()))
                .withLocalCompose(true)
                .withPull(true)
                .withEnv(
                        Stream.of(
                                new AbstractMap.SimpleImmutableEntry<>("CIPHERDUCK_HUB_VERSION", CIPHERDUCK_HUB_VERSION),
                                new AbstractMap.SimpleImmutableEntry<>("CIPHERDUCK_KEYCLOAK_VERSION", CIPHERDUCK_KEYCLOAK_VERSION),
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTP_PORT", Integer.toString(keycloakServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("KEYCLOAK_HTTPS_PORT", "8443"),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_PORT", Integer.toString(minioServicePort)),
                                new AbstractMap.SimpleImmutableEntry<>("MINIO_CONSOLE_PORT", Integer.toString(minioConsolePort)),
                                new AbstractMap.SimpleImmutableEntry<>("HUB_PORT", Integer.toString(hubPort))
                        ).collect(Collectors.toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue)))
                .withLogConsumer("minio-1", outputFrame -> log.debug("[minio_1] " + outputFrame.getUtf8String()))
                .withExposedService("minio-1", minioServicePort, Wait.forListeningPort())
                .withExposedService("keycloak-1", keycloakServicePort, Wait.forListeningPort())
                .withExposedService("hub-1", hubPort, Wait.forListeningPort())
                .waitingFor("minio_setup-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)))
                .waitingFor("hub_setup_storage_profile-1", new LogMessageWaitStrategy().withRegEx(".*createbuckets successful.*").withStartupTimeout(Duration.ofMinutes(2)));
        compose.start();
        log.info(String.format("Done setup docker %s", dockerConfig));
    }
}
