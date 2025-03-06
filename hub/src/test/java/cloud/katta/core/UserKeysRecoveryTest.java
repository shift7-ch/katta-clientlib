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
import com.nimbusds.jose.JOSEException;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({HubTestSetupDockerExtension.UnattendedLocalOnly.class})
public class UserKeysRecoveryTest extends AbstractHubTest {

    private Stream<Arguments> arguments() {
        return Stream.of(minioSTSUnattendedLocalOnly);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void firstLoginAndUserKeyRecovery(final HubTestConfig hubTestConfig) throws Exception {
        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        final UsersResourceApi usersApi = new UsersResourceApi(hubSession.getClient());
        final UserDto me = usersApi.apiUsersMeGet(true);

        final JOSEException exception = assertThrows(JOSEException.class, () -> UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(),
                new AlphanumericRandomStringService().random()));
        assertInstanceOf(InvalidKeyException.class, exception.getCause());
        assertNotNull(UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(),
                hubTestConfig.setup.userConfig.setupCode));
    }
}
