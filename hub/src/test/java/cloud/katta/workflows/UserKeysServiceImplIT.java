/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.PasswordStoreFactory;
import cloud.katta.client.api.DeviceResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.DeviceDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestSetupDockerExtension;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@ExtendWith({HubTestSetupDockerExtension.Local.class})
class UserKeysServiceImplIT extends AbstractHubTest {

    private static Stream<Arguments> arguments() {
        // static or STS does not matter
        return Stream.of(LOCAL_MINIO_STATIC);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testSetupNewDeviceWithAccountKeyForExistingUserKeys(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);

        final DeviceKeys existingDeviceKeys = new DeviceKeysServiceImpl(PasswordStoreFactory.get()).getOrCreateDeviceKeys(hubSession.getHost(), deviceSetupCallback(config.setup));
        final UserKeys expecteduserKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(), existingDeviceKeys);

        // N.B. DeviceKeysServiceImpl does not override device keys in keychain, so compare remote
        final int numDevicesBeforeRecover = new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size();

        // setting up new device w/ Account Key for existing user keys and correct setup code
        final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getOrCreateUserKeys(hubSession.getHost(), hubSession.getMe(), DeviceKeys.create(), deviceSetupCallback(config.setup));
        assertEquals(expecteduserKeys, userKeys);

        final int numDevicesAfterRecover = new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size();
        assertEquals(numDevicesBeforeRecover + 1, numDevicesAfterRecover);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testFailSetupNewDeviceWithAccountKeyForExistingUserKeys(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);

        // Setting up new device w/ Account Key for existing user keys with erroneous setup code
        final SecurityFailure securityException = assertThrows(SecurityFailure.class, () -> new UserKeysServiceImpl(hubSession).getOrCreateUserKeys(hubSession.getHost(), hubSession.getMe(), DeviceKeys.create(), deviceSetupCallback(new HubTestConfig.Setup().withUserConfig(new HubTestConfig.Setup.UserConfig("alice", "wonderland", "in")))));
        assertEquals(JOSEException.class, securityException.getCause().getClass());
        assertEquals("checksum failed", securityException.getCause().getCause().getMessage());
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testSetupExistingDeviceWithAccountKeyForExistingUserKeys(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);

        final DeviceKeys existingDeviceKeys = new DeviceKeysServiceImpl(PasswordStoreFactory.get()).getOrCreateDeviceKeys(hubSession.getHost(), deviceSetupCallback(config.setup));
        final UserKeys expecteduserKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(), existingDeviceKeys);

        // delete devices remote in order to simplify checking new device uploaded
        for (final DeviceDto device : new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices()) {
            new DeviceResourceApi(hubSession.getClient()).apiDevicesDeviceIdDelete(device.getId());
        }
        assertEquals(0, new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size());

        // setting up existing device w/ Account Key for existing user keys (if device keys from keychain not present in hub)
        final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getOrCreateUserKeys(hubSession.getHost(), hubSession.getMe(), DeviceKeys.create(), deviceSetupCallback(config.setup));
        assertEquals(expecteduserKeys, userKeys);

        final int numDevicesAfterRecover = new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size();
        assertEquals(1, numDevicesAfterRecover);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void testSetupNewUserKeysAndAccountKey(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        final UserDto me = hubSession.getMe();

        final DeviceKeys existingDeviceKeys = new DeviceKeysServiceImpl(PasswordStoreFactory.get()).getOrCreateDeviceKeys(hubSession.getHost(), deviceSetupCallback(config.setup));
        final UserKeys expecteduserKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(), existingDeviceKeys);

        // delete devices remote in order to simplify checking new device uploaded
        for (final DeviceDto device : new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices()) {
            new DeviceResourceApi(hubSession.getClient()).apiDevicesDeviceIdDelete(device.getId());
        }
        assertEquals(0, new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size());

        for (final DeviceDto device : new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices()) {
            new DeviceResourceApi(hubSession.getClient()).apiDevicesDeviceIdDelete(device.getId());
        }

        final UserDto newMe = new UserDto().id(me.getId());
        new UsersResourceApi(hubSession.getClient()).apiUsersMePut(newMe);
        new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true);

        // setting up new user keys and account key
        final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getOrCreateUserKeys(hubSession.getHost(), newMe, DeviceKeys.create(), deviceSetupCallback(new HubTestConfig.Setup().withUserConfig(new HubTestConfig.Setup.UserConfig("alice", "wonderland", "in"))));

        assertNotEquals(expecteduserKeys, userKeys);

        new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true);

        final int numDevicesAfterRecover = new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true).getDevices().size();
        assertEquals(1, numDevicesAfterRecover);

        // restore setup code (to prevent side-effect if in LocalAlreadyRunning mode)
        new UsersResourceApi(hubSession.getClient()).apiUsersMePut(me);
    }
}
