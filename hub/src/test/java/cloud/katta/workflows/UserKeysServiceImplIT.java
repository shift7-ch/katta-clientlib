/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;

@ExtendWith({HubTestSetupDockerExtension.LocalAlreadyRunning.class})
class UserKeysServiceImplIT extends AbstractHubTest {

    private static Stream<Arguments> arguments() {
        return Stream.of(LOCAL_MINIO_STATIC);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testSetupCode(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true);
    }
}
