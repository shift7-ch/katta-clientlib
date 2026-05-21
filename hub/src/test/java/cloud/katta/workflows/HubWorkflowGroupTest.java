/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import cloud.katta.testsetup.HubTestSetupDockerExtension;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Create vault and share vault with a group. Serves as hub API integration/regression test.
 * As we want to simulate admin's first login only after a vault is created by alice and shared with admin,
 * we do not start a hub session for admin.
 */
class HubWorkflowGroupTest {

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    class LocalStatic extends AbstractHubWorkflowGroupTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(LOCAL_MINIO_STATIC);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    class LocalSTS extends AbstractHubWorkflowGroupTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(LOCAL_MINIO_STS);
        }
    }
}
