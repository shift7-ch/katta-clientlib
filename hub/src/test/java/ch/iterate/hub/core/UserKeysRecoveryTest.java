package ch.iterate.hub.core;

import ch.cyberduck.core.AlphanumericRandomStringService;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.InvalidKeyException;
import java.util.stream.Stream;

import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestConfig;
import ch.iterate.hub.testsetup.HubTestSetupDockerExtension;
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
