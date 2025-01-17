/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

import ch.iterate.hub.testsetup.HubTestUtilities;
import ch.iterate.hub.testsetup.docker_setup.UnattendedLocalKeycloakDev;

import static ch.iterate.hub.testsetup.HubTestSetupConfigs.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Test synchronization of profiles, adding profiles and adding vaults.
 * Same local context (profiles, hub host collection) shared across storage profile and tests.
 */
// TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 test01 needs to be run only once, actually
public class HubSynchronizeTest {
    @Nested
    @ExtendWith({ch.iterate.hub.testsetup.docker_setup.UnattendedLocalOnly.class})
    @TestInstance(PER_CLASS)
    public class UnattendedLocalOnly extends AbstractHubSynchronizeTest {
        private Stream<Arguments> arguments() throws Exception {
            return Stream.of(minioStaticUnattendedLocalOnly, minioSTSUnattendedLocalOnly);
        }
    }

    @Nested
    @ExtendWith({UnattendedLocalKeycloakDev.class})
    @TestInstance(PER_CLASS)
    @Disabled("TODO https://github.com/shift7-ch/cipherduck-hub/issues/12 implemented unattended keycloak dev with aws in ci, dedicated keycloak?")
    public class UnattendedLocalKeycloakDevOnlyStatic extends AbstractHubSynchronizeTest {
        @BeforeAll
        public void setup() {
            HubTestUtilities.preferences();
        }

        private Stream<Arguments> arguments() throws Exception {
            return Stream.of();
        }
    }


    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedLocalOnly extends AbstractHubSynchronizeTest {
        @BeforeAll
        public void setup() {
            HubTestUtilities.preferences();
        }

        private Stream<Arguments> arguments() throws Exception {
            return Stream.of(minioStaticAttendedLocalOnly, minioSTSAttendedLocalOnly);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedKeycloakTesting extends AbstractHubSynchronizeTest {
        @BeforeAll
        public void setup() {
            HubTestUtilities.preferences();
        }

        private Stream<Arguments> arguments() throws Exception {
            return Stream.of(
                    minioStaticAttendedLocalKeycloadkDev
                    , minioSTSAttendedLocalKeycloadkDev
                    , awsSTSAttendedLocalKeycloadkDev
                    , awsStaticAttendedLocalKeycloadkDev
            );
        }
    }
}
