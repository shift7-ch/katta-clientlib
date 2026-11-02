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
 * Grant access as a vault member without the OWNER role. The storage profile is irrelevant for the access grant, so
 * this runs for a single profile only.
 */
class HubMemberGrantTest {

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({HubTestSetupDockerExtension.Local.class})
    class LocalStatic extends AbstractHubMemberGrantTest {
        private Stream<Arguments> arguments() {
            return Stream.of(LOCAL_MINIO_STATIC);
        }
    }
}
