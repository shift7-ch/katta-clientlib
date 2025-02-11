/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import ch.iterate.hub.testsetup.HubTestSetupDockerExtension;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Test synchronization of profiles, adding profiles and adding vaults.
 * Same local context (profiles, hub host collection) shared across storage profile and tests.
 */
class HubSynchronizeTest {

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.UnattendedLocalOnly.class})
    @TestInstance(PER_CLASS)
    public class UnattendedMinio extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(minioStaticUnattendedLocalOnly, minioSTSUnattendedLocalOnly);
        }
    }

    @Nested
    @ExtendWith({HubTestSetupDockerExtension.UnattendedLocalKeycloakDev.class})
    @TestInstance(PER_CLASS)
    @Disabled("TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 implemented unattended keycloak dev with aws in ci, dedicated keycloak?")
    public class UnattendedLocalKeycloakDevOnlyStatic extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of();
        }
    }


    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedMinio extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(minioStaticAttendedLocalOnly, minioSTSAttendedLocalOnly);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedKeycloakTesting extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() {
            return Stream.of(
                    minioStaticAttendedLocalKeycloadkDev,
                    minioSTSAttendedLocalKeycloadkDev,
                    awsSTSAttendedLocalKeycloadkDev,
                    awsStaticAttendedLocalKeycloadkDev
            );
        }
    }
}
