/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.testsetup.model;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class HubTestSetupConfig {

    private String hubURL;
    private HubTestSetupUserConfig ADMIN;
    private HubTestSetupUserConfig USER;

    private Stream<Arguments> configs;
    private DockerConfig dockerConfig;

    @Override
    public String toString() {
        return "HubTestSetupConfig{" +
                "hubURL='" + hubURL + '\'' +
                ", ADMIN=" + ADMIN +
                ", USER=" + USER +
                '}';
    }

    public String hubURL() {
        return hubURL;
    }

    public HubTestSetupConfig withHubURL(final String hubURL) {
        this.hubURL = hubURL;
        return this;
    }

    public HubTestSetupUserConfig ADMIN() {
        return ADMIN;
    }

    public HubTestSetupConfig withADMIN(final HubTestSetupUserConfig ADMIN) {
        this.ADMIN = ADMIN;
        return this;
    }

    public HubTestSetupUserConfig USER_001() {
        return USER;
    }

    public HubTestSetupConfig withUSER(final HubTestSetupUserConfig USER) {
        this.USER = USER;
        return this;
    }


    public Stream<Arguments> configs() {
        return configs;
    }

    public HubTestSetupConfig withConfigs(final Stream<Arguments> configs) {
        this.configs = configs;
        return this;
    }

    public DockerConfig dockerConfig() {
        return dockerConfig;
    }

    public HubTestSetupConfig withDockerConfig(final DockerConfig dockerConfig) {
        this.dockerConfig = dockerConfig;
        return this;
    }

    public static class DockerConfig {
        public final String composeFile;
        public final int keycloakServicePort;
        public final int minioServicePort;
        public final int minioConsolePort;
        public final int hubPort;

        public DockerConfig(final String composeFile, final int keycloakServicePort, final int minioServicePort, final int minioConsolePort, final int hubPort) {
            this.composeFile = composeFile;
            this.keycloakServicePort = keycloakServicePort;
            this.minioServicePort = minioServicePort;
            this.minioConsolePort = minioConsolePort;
            this.hubPort = hubPort;
        }
    }
}
