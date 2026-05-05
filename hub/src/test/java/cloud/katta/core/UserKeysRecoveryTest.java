/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.AlphanumericRandomStringService;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.InvalidKeyException;
import java.util.stream.Stream;

import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({HubTestSetupDockerExtension.Local.class})
class UserKeysRecoveryTest extends AbstractHubTest {

    private Stream<Arguments> arguments() {
        return Stream.of(LOCAL_MINIO_STS);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void testFirstLoginAndUserKeyRecovery(final HubTestConfig hubTestConfig) throws Exception {
        final HubSession hubSession = setupConnection(hubTestConfig);
        final UsersResourceApi usersApi = new UsersResourceApi(hubSession.getClient());
        final UserDto me = usersApi.apiUsersMeGet(true, false);

        final SecurityFailure exception = assertThrows(SecurityFailure.class, () -> UserKeys.recoverWithAccountKey(me.getPrivateKeys(), new AlphanumericRandomStringService().random(), me.getEcdhPublicKey(), me.getEcdsaPublicKey()
        ));
        assertInstanceOf(JOSEException.class, exception.getCause());
        assertInstanceOf(InvalidKeyException.class, exception.getCause().getCause());
        assertNotNull(UserKeys.recoverWithAccountKey(me.getPrivateKeys(), hubTestConfig.setup.userConfig.setupCode, me.getEcdhPublicKey(), me.getEcdsaPublicKey()
        ));
    }
}
