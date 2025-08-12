/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.Credentials;
import ch.cyberduck.core.Host;
import ch.cyberduck.core.Local;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.profiles.LocalProfilesFinder;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import cloud.katta.client.ApiException;
import cloud.katta.client.HubApiClient;
import cloud.katta.client.api.ConfigResourceApi;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.StorageResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.ConfigDto;
import cloud.katta.client.model.Protocol;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubProtocol;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.s3.S3AssumeRoleProtocol;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.workflows.exceptions.AccessException;
import cloud.katta.workflows.exceptions.SecurityFailure;
import com.nimbusds.jose.JOSEException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

class CreateVaultServiceTest {

    @Test
    void createVault() throws AccessException, SecurityFailure, BackgroundException, ApiException, JOSEException, IOException, URISyntaxException {
        final HubSession hubSession = Mockito.mock(HubSession.class);
        final Host hub = Mockito.mock(Host.class);
        final VaultResourceApi vaults = Mockito.mock(VaultResourceApi.class);
        final UsersResourceApi users = Mockito.mock(UsersResourceApi.class);
        final ConfigResourceApi config = Mockito.mock(ConfigResourceApi.class);

        final UserKeys userKeys = UserKeys.create();

        final HubApiClient apiClient = Mockito.mock(HubApiClient.class);
        final StorageProfileResourceApi storageProfiles = Mockito.mock(StorageProfileResourceApi.class);
        final StorageResourceApi storage = Mockito.mock(StorageResourceApi.class);

        final UUID vaultId = UUID.randomUUID();
        final UUID storageProfileId = UUID.randomUUID();
        final StorageProfileDto storageProfile = new StorageProfileDto(
                new StorageProfileS3STSDto()
                        .id(storageProfileId)
                        .protocol(Protocol.S3_STS)
                        .stsEndpoint("http://audley.end.point")

        );
        final StorageProfileDtoWrapper storageProfileWrapper = StorageProfileDtoWrapper.coerce(storageProfile);

        Mockito.when(hubSession.getHost()).thenReturn(hub);
        Mockito.when(hub.getProtocol()).thenReturn(new HubProtocol() {
            @Override
            public String getOAuthTokenUrl() {
                return "http://tok-tok.dev.null/auth/token";
            }
        });
        Mockito.when(hub.getCredentials()).thenReturn(new Credentials());
        Mockito.when(hub.getHostname()).thenReturn("storage");
        Mockito.when(apiClient.getBasePath()).thenReturn("http://nix.com/api");
        Mockito.when(vaults.getApiClient()).thenReturn(apiClient);

        Mockito.when(storageProfiles.apiStorageprofileProfileIdGet(storageProfileId)).thenReturn(storageProfile);
        Mockito.when(config.apiConfigGet()).thenReturn(new ConfigDto().keycloakClientIdCryptomatorVaults("hex"));

        final UserDto me = new UserDto();
        Mockito.when(users.apiUsersMeGet(false, false)).thenReturn(me);

        final ProtocolFactory factory = ProtocolFactory.get();
        // Register parent protocol definitions
        factory.register(
                new HubProtocol(),
                new S3AssumeRoleProtocol("PasswordGrant")
        );
        // Load bundled profiles
        factory.load(new LocalProfilesFinder(factory, new Local(AbstractHubTest.class.getResource("/").toURI().getPath())));

        final CreateVaultService.CreateVaultModel createVaultModel = new CreateVaultService.CreateVaultModel(vaultId, null, null, null, null, null, null, null, true, 66);

        final CreateVaultService createVaultService = new CreateVaultService(hubSession, config, vaults, storageProfiles, users, storage, CreateVaultService.TemplateUploadService.disabled, CreateVaultService.STSInlinePolicyService.disabled);

        createVaultService.createVault(userKeys, storageProfileWrapper, createVaultModel);

        Mockito.verify(vaults, times(1)).apiVaultsVaultIdPut(eq(vaultId), any());
        Mockito.verify(vaults, times(1)).apiVaultsVaultIdAccessTokensPost(eq(vaultId), any());
        Mockito.verify(storage, times(1)).apiStorageVaultIdPut(eq(vaultId), any());
    }
}
