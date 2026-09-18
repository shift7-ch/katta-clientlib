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

import java.io.IOException;
import java.net.URISyntaxException;

import cloud.katta.client.ApiException;


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

    protected void setupDocker(final HubTestConfig.Setup.DockerConfig dockerConfig) throws IOException {
        log.info("Setup docker {}", dockerConfig);
        compose = KattaCompose.container(dockerConfig.envFile, dockerConfig.profile);
        compose.start();
        log.info("Done setup docker {}", dockerConfig);
    }

    /**
     * Add the test configuration to the realm of katta-compose, which is not used by the hybrid profile with an existing Keycloak.
     */
    protected void setupRealm(final HubTestConfig.Setup setup) throws IOException {
        log.info("Setup realm for {}", setup.dockerConfig);
        try {
            KattaTestRealm.setup(KattaCompose.properties(setup.dockerConfig.envFile), setup);
        }
        catch(ApiException e) {
            throw new IOException(e);
        }
    }

    /**
     * Local
     */
    public static class Local extends HubTestSetupDockerExtension {
        @Override
        public void beforeAll(final ExtensionContext context) throws URISyntaxException, IOException {
            this.setupDocker(AbstractHubTest.LOCAL_DOCKER_CONFIG);
            this.setupRealm(AbstractHubTest.LOCAL_TEST_CONFIG);
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
            this.setupRealm(AbstractHubTest.LOCAL_TEST_CONFIG);
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
            this.setupDocker(AbstractHubTest.CHIPOTLE_DOCKER_CONFIG);
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
            this.setupDocker(AbstractHubTest.CHIPOTLE_DOCKER_CONFIG);
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
