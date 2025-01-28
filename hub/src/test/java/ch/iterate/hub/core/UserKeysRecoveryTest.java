package ch.iterate.hub.core;

import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.docker_setup.UnattendedLocalOnly;
import ch.iterate.hub.testsetup.model.HubTestConfig;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.InvalidKeyException;
import java.util.stream.Stream;

import static ch.iterate.hub.testsetup.HubTestSetupConfigs.minioSTSUnattendedLocalOnly;
import static ch.iterate.hub.testsetup.HubTestUtilities.setupForUser;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({UnattendedLocalOnly.class})
public class UserKeysRecoveryTest extends AbstractHubTest {

    private Stream<Arguments> arguments() {
        return Stream.of(minioSTSUnattendedLocalOnly);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void firstLoginAndUserKeyRecovery(final HubTestConfig hubTestConfig) throws Exception {
        final HubSession hubSession = setupForUser(hubTestConfig.hubTestSetupConfig, hubTestConfig.hubTestSetupConfig.USER_001());
        final UsersResourceApi usersApi = new UsersResourceApi(hubSession.getClient());
        final UserDto me = usersApi.apiUsersMeGet(true);

        final JOSEException exc = assertThrows(JOSEException.class, () -> UserKeys.recover(me.getEcdhPublicKey(), me.getEcdsaPublicKey(), me.getPrivateKey(), hubTestConfig.hubTestSetupConfig.USER_001().setupCode));
        assertTrue(exc.getCause() instanceof InvalidKeyException);
    }
}
