/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import cloud.katta.testsetup.HubTestSetupDockerExtension;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Test synchronization of profiles, adding profiles and adding vaults.
 * Same local context (profiles, hub host collection) shared across storage profile and tests.
 */
class HubSynchronizeTest {

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    @TestInstance(PER_CLASS)
    class Local extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.LocalKeepRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    class LocalKeepRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.LocalAlreadyRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    class LocalAlreadyRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.HybridTesting.class})
    @TestInstance(PER_CLASS)
    class Hybrid extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(CHIPOTLE_MINIO_STATIC, CHIPOTLE_MINIO_STS, CHIPOTLE_AWS_STATIC, CHIPOTLE_AWS_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.HybridTestingKeepRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    class HybridKeepRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(CHIPOTLE_MINIO_STATIC, CHIPOTLE_MINIO_STS, CHIPOTLE_AWS_STATIC, CHIPOTLE_AWS_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.HybridTestingAlreadyRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    class HybridAlreadyRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(CHIPOTLE_MINIO_STATIC, CHIPOTLE_MINIO_STS, CHIPOTLE_AWS_STATIC, CHIPOTLE_AWS_STS);
        }
    }
}
