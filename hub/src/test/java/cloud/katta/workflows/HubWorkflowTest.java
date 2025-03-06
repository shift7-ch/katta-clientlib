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
 * Create vault and share vault. Serves as hub API integration/regression test.
 * Local context (profiles, hub host collection) etc. is for user alice only.
 * Only remote hub calls are done for admin user.
 */
class HubWorkflowTest {

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.UnattendedLocalOnly.class})
    public class UnattendedMinio extends AbstractHubWorkflowTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(minioStaticUnattendedLocalOnly);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.UnattendedLocalOnly.class})
    public class UnattendedMinioSTS extends AbstractHubWorkflowTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(minioSTSUnattendedLocalOnly);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedMinio extends AbstractHubWorkflowTest {
        private Stream<Arguments> arguments() {
            // Needs to be run separately for every storage profile because of the hard-coded vault counts.
            return Stream.of(minioStaticAttendedLocalOnly, minioSTSAttendedLocalOnly);
        }
    }
}
