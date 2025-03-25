/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

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
    public class Local extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.LocalKeepRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    public class LocalKeepRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.LocalAlreadyRunning.class})
    @TestInstance(PER_CLASS)
    @Disabled
    public class LocalAlreadyRunning extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC, LOCAL_MINIO_STS);
        }
    }
}
