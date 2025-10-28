/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
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
 * Create vault and share vault. Serves as hub API integration/regression test.
 * Local context (profiles, hub host collection) etc. is for user alice only.
 * Only remote hub calls are done for admin user.
 */
class HubWorkflowTest {

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    class LocalStatic extends AbstractHubWorkflowTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(LOCAL_MINIO_STATIC);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    class LocalSTS extends AbstractHubWorkflowTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(LOCAL_MINIO_STS);
        }
    }
}
