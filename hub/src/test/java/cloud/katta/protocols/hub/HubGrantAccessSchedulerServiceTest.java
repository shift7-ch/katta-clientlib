/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.protocols.hub;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.HostPasswordStore;
import ch.cyberduck.core.exception.BackgroundException;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.HubApiClient;
import cloud.katta.client.api.DeviceResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.DeviceDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.Type1;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.DeviceKeys;
import cloud.katta.crypto.UserKeys;
import cloud.katta.protocols.hub.HubGrantAccessSchedulerService;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.workflows.GrantAccessService;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jose.JOSEException;

import static cloud.katta.crypto.KeyHelper.encodePrivateKey;
import static cloud.katta.crypto.KeyHelper.encodePublicKey;
import static cloud.katta.workflows.DeviceKeysServiceImpl.KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME;
import static cloud.katta.workflows.DeviceKeysServiceImpl.KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

class HubGrantAccessSchedulerServiceTest {

    @Test
    public void testOperate() throws BackgroundException, ApiException, JOSEException, JsonProcessingException, AccessException, SecurityFailure {
        final HostPasswordStore keychain = Mockito.mock(HostPasswordStore.class);
        final HubSession hubSession = Mockito.mock(HubSession.class);
        final Host hub = Mockito.mock(Host.class);
        final VaultResourceApi vaults = Mockito.mock(VaultResourceApi.class);
        final UsersResourceApi users = Mockito.mock(UsersResourceApi.class);
        final DeviceResourceApi devices = Mockito.mock(DeviceResourceApi.class);
        final GrantAccessService grants = Mockito.mock(GrantAccessService.class);

        final UserKeys userKeys = UserKeys.create();
        final DeviceKeys deviceKeys = DeviceKeys.create();

        final HubApiClient apiClient = Mockito.mock(HubApiClient.class);

        Mockito.when(hubSession.getHost()).thenReturn(hub);

        Mockito.when(hubSession.getClient()).thenReturn(apiClient);
        Mockito.when(keychain.getPassword(eq(KEYCHAIN_PUBLIC_DEVICE_KEY_ACCOUNT_NAME), eq("Fritzl@Unterwittelsbach"))).thenReturn(encodePublicKey(deviceKeys.getEcKeyPair().getPublic()));
        Mockito.when(keychain.getPassword(eq(KEYCHAIN_PRIVATE_DEVICE_KEY_ACCOUNT_NAME), eq("Fritzl@Unterwittelsbach"))).thenReturn(encodePrivateKey(deviceKeys.getEcKeyPair().getPrivate()));
        Mockito.when(hubSession.getMe()).thenReturn(new UserDto()
                .ecdhPublicKey(encodePublicKey(userKeys.ecdhKeyPair().getPublic()))
                .ecdsaPublicKey(encodePublicKey(userKeys.ecdsaKeyPair().getPublic()))
        );
        Mockito.when(hub.getCredentials()).thenReturn(new Credentials().setUsername("Fritzl"));
        Mockito.when(hub.getHostname()).thenReturn("Unterwittelsbach");
        Mockito.when(devices.apiDevicesDeviceIdGet(any())).thenReturn(new DeviceDto()
                .name("Franzl")
                .publicKey(Base64.getEncoder().encodeToString(deviceKeys.getEcKeyPair().getPublic().getEncoded()))
                .userPrivateKey(userKeys.encryptForDevice(deviceKeys.getEcKeyPair().getPublic()))
                .type(Type1.DESKTOP)
                .creationTime(new DateTime()));
        final UUID vaultId = UUID.randomUUID();
        Mockito.when(vaults.apiVaultsAccessibleGet(Role.OWNER)).thenReturn(Arrays.asList(
                new VaultDto().archived(true), new VaultDto().archived(false).id(vaultId), new VaultDto().archived(null)));

        final HubGrantAccessSchedulerService service = new HubGrantAccessSchedulerService(hubSession, keychain, vaults, users, devices, grants);
        service.operate(null);

        Mockito.verify(grants, times(1)).grantAccessToUsersRequiringAccessGrant(eq(vaultId), any());
    }
}
