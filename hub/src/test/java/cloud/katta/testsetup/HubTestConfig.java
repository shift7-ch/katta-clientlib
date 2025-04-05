/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.testsetup;

public class HubTestConfig {
    public final Setup setup;
    public final VaultSpec vault;

    public HubTestConfig(final Setup setup, final VaultSpec vault) {
        this.setup = setup;
        this.vault = vault;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HubTestConfig{");
        sb.append("hubTestSetupConfig=").append(setup);
        sb.append(", vaultSpec=").append(vault);
        sb.append('}');
        return sb.toString();
    }

    public static class Setup {
        public String hubURL;
        public String clientId = "cryptomator";
        public UserConfig adminConfig;
        public UserConfig userConfig;
        public DockerConfig dockerConfig;

        public Setup withHubURL(final String hubURL) {
            this.hubURL = hubURL;
            return this;
        }

        public Setup withAdminConfig(final UserConfig adminConfig) {
            this.adminConfig = adminConfig;
            return this;
        }

        public Setup withUserConfig(final UserConfig userConfig) {
            this.userConfig = userConfig;
            return this;
        }

        public Setup withDockerConfig(final DockerConfig dockerConfig) {
            this.dockerConfig = dockerConfig;
            return this;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("HubTestSetupConfig{");
            sb.append("hubURL='").append(hubURL).append('\'');
            sb.append(", adminConfig=").append(adminConfig);
            sb.append(", userConfig=").append(userConfig);
            sb.append(", dockerConfig=").append(dockerConfig);
            sb.append('}');
            return sb.toString();
        }

        public static class DockerConfig {
            public final String composeFile;
            public final int keycloakServicePort;
            public final int minioServicePort;
            public final int minioConsolePort;
            public final int hubPort;
            public final String profile;
            public final String hubKeycloakUrl;
            public final String hubKeycloakBasePath;
            public final String hubKeycloakRealm;
            public final String hubAdminUser;
            public final String hubAdminPassword;


            public DockerConfig(final String composeFile, final int keycloakServicePort, final int minioServicePort, final int minioConsolePort, final int hubPort, final String profile, final String hubKeycloakUrl, final String hubKeycloakBasePath, final String hubKeycloakRealm, final String hubAdminUser, final String hubAdminPassword) {
                this.composeFile = composeFile;
                this.keycloakServicePort = keycloakServicePort;
                this.minioServicePort = minioServicePort;
                this.minioConsolePort = minioConsolePort;
                this.hubPort = hubPort;
                this.profile = profile;
                this.hubKeycloakUrl = hubKeycloakUrl;
                this.hubKeycloakBasePath = hubKeycloakBasePath;
                this.hubKeycloakRealm = hubKeycloakRealm;
                this.hubAdminUser = hubAdminUser;
                this.hubAdminPassword = hubAdminPassword;
            }
        }

        public static class UserConfig {
            public final String username;
            public final String password;
            public final String setupCode;

            public UserConfig(final String username, final String password, final String setupCode) {
                this.username = username;
                this.password = password;
                this.setupCode = setupCode;
            }

            @Override
            public String toString() {
                final StringBuilder sb = new StringBuilder("HubTestSetupUserConfig{");
                sb.append("username='").append(username).append('\'');
                sb.append(", password='").append(password).append('\'');
                sb.append(", setupCode='").append(setupCode).append('\'');
                sb.append('}');
                return sb.toString();
            }
        }
    }

    public static class VaultSpec {
        public final String storageProfileName;
        public final String storageProfileId;
        public final String bucketName;
        public final String username;
        public final String password;
        public final String region;

        public VaultSpec(final String storageProfileName, final String storageProfileId, final String bucketName, final String username, final String password, final String region) {
            this.storageProfileName = storageProfileName;
            this.storageProfileId = storageProfileId;
            this.bucketName = bucketName;
            this.username = username;
            this.password = password;
            this.region = region;
        }
    }
}
