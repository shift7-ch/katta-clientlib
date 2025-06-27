/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.SetupCodeJWE;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.MethodIgnorableSource;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractHubWorkflowTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubWorkflowTest.class.getName());

    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void testHubWorkflow(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        try {
            checkNumberOfVaults(hubSession, config, null, 0, 0, 0, 0, -1);

            final HubTestConfig.Setup setup = config.setup;
            log.info("S01 {} alice creates vault", setup);
            final ApiClient adminApiClient = getAdminApiClient(setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();

            final UUID vaultId = UUID.randomUUID();
            final boolean automaticAccessGrant = true;
            // upload template (STS: create bucket first, static: existing bucket)
            // TODO test with multiple wot levels?

            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost()));
            new CreateVaultService(hubSession).createVault(userKeys, storageProfileWrapper,
                    new CreateVaultService.CreateVaultModel(vaultId,
                            "vault", null,
                            config.vault.storageProfileId, config.vault.username, config.vault.password, config.vault.bucketName,
                            config.vault.region, automaticAccessGrant, 3));
            checkNumberOfVaults(hubSession, config, vaultId, 0, 0, 1, 0, 0);

            log.info("S02 {} alice shares vault with admin as owner", setup);
            final List<UserDto> userDtos = new UsersResourceApi(hubSession.getClient()).apiUsersGet();
            String adminId = null;
            for(final UserDto user : userDtos) {
                if("admin".equals(user.getName())) {
                    adminId = user.getId();
                    break;
                }
            }
            new VaultResourceApi(hubSession.getClient()).apiVaultsVaultIdUsersUserIdPut(adminId, vaultId, Role.OWNER);
            checkNumberOfVaults(hubSession, config, vaultId, 1, 0, 1, 0, 0);

            log.info("S03 {} admin uploads user keys", setup);
            final UserKeys adminKeys = UserKeys.create();
            final String adminAccountKey = config.setup.adminConfig.setupCode;
            final UsersResourceApi users = new UsersResourceApi(adminApiClient);

            // TODO https://github.com/shift7-ch/katta-server/issues/4 bad code smell - encapsulate initial setup
            final UserDto admin = users.apiUsersMeGet(false, false)
                    .ecdhPublicKey(adminKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(adminKeys.encodedEcdsaPublicKey())
                    .privateKey(adminKeys.encryptWithSetupCode(adminAccountKey))
                    .setupCode(new SetupCodeJWE(adminAccountKey).encryptForUser(adminKeys.ecdhKeyPair().getPublic()));
            users.apiUsersMePut(admin);
            checkNumberOfVaults(hubSession, config, vaultId, 1, 0, 1, 0, 1);

            log.info("S04 {} alice adds trust to admin", setup);
            new WoTServiceImpl(users).sign(new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost())), admin);

            log.info("S04 {} alice grants access to admin", setup);
            new GrantAccessServiceImpl(hubSession).grantAccessToUsersRequiringAccessGrant(vaultId, userKeys);
            checkNumberOfVaults(hubSession, config, vaultId, 1, 0, 1, 0, 0);
        }
        finally {
            hubSession.close();
        }
    }

    private static void checkNumberOfVaults(final HubSession hubSession, final HubTestConfig hubTestSetup, final UUID vaultIdSharedWithAdmin,
                                            final int adminOwner, final int adminMember, final int aliceOwner, final int aliceMember, final int nbUsersRequiringAccessGrant) throws ApiException, IOException {
        final VaultResourceApi vaultResourceApialice = new VaultResourceApi(hubSession.getClient());
        final List<VaultDto> vaultaliceOwned = vaultResourceApialice.apiVaultsAccessibleGet(Role.OWNER);
        for(final VaultDto vaultDto : vaultaliceOwned) {
            log.info("owned by alice {}", vaultDto);
        }
        assertEquals(aliceOwner, vaultaliceOwned.size(), "alice OWNER");

        final List<VaultDto> vaultaliceMember = vaultResourceApialice.apiVaultsAccessibleGet(Role.MEMBER);
        for(final VaultDto vaultDto : vaultaliceMember) {
            log.info("alice member of {}", vaultDto);
        }
        assertEquals(aliceMember, vaultResourceApialice.apiVaultsAccessibleGet(Role.MEMBER).size(), "alice MEMBER");

        final ApiClient adminApiClient = getAdminApiClient(hubTestSetup.setup);
        final VaultResourceApi vaultResourceApiAdmin = new VaultResourceApi(adminApiClient);
        final List<VaultDto> vaultsAdminOwned = vaultResourceApiAdmin.apiVaultsAccessibleGet(Role.OWNER);
        for(final VaultDto vaultDto : vaultsAdminOwned) {
            log.info("owned by admin {}", vaultDto);
        }
        assertEquals(adminOwner, vaultsAdminOwned.size(), "admin OWNER");

        final List<VaultDto> vaultsAdminMember = vaultResourceApiAdmin.apiVaultsAccessibleGet(Role.MEMBER);
        for(final VaultDto vaultDto : vaultsAdminMember) {
            log.info("admin member of {}", vaultDto);
        }
        assertEquals(adminMember, vaultsAdminMember.size(), "admin MEMBER");

        if(nbUsersRequiringAccessGrant >= 0) {
            final List<MemberDto> usersRequiringAccessGrant = vaultResourceApialice.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultIdSharedWithAdmin);
            for(final MemberDto memberDto : usersRequiringAccessGrant) {
                log.info("user requiring access to {}: {}", vaultIdSharedWithAdmin, memberDto);
            }
            assertEquals(nbUsersRequiringAccessGrant, usersRequiringAccessGrant.size());
        }
    }
}
