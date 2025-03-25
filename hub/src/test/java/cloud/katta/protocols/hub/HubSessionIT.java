/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.exception.InteroperabilityException;
import ch.cyberduck.core.preferences.PreferencesFactory;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({HubTestSetupDockerExtension.Local.class})
class HubSessionIT extends AbstractHubTest {

    private static Stream<Arguments> arguments() {
        // static or STS does not matter
        return Stream.of(LOCAL_MINIO_STATIC);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testMinApiLevel(final HubTestConfig config) {
        PreferencesFactory.get().setProperty("cloud.katta.min_api_level", 5);
        final InteroperabilityException exception = assertThrows(InteroperabilityException.class, () -> setupConnection(config.setup));
        assertTrue(exception.getDetail().startsWith("Client requires API level at least 5, found 4, for"));
    }
}
