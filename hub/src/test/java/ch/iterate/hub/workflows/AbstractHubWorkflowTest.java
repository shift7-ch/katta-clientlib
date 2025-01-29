/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.features.Home;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.api.UsersResourceApi;
import ch.iterate.hub.client.api.VaultResourceApi;
import ch.iterate.hub.client.model.MemberDto;
import ch.iterate.hub.client.model.Role;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.UserDto;
import ch.iterate.hub.client.model.VaultDto;
import ch.iterate.hub.core.FirstLoginDeviceSetupCallback;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.SetupCodeJWE;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.HubStorageProfileListService;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestConfig;
import ch.iterate.hub.testsetup.MethodIgnorableSource;

import static ch.iterate.hub.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractHubWorkflowTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubWorkflowTest.class.getName());

    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void testHubWorkflow(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        try {
            checkNumberOfVaults(hubSession, config, null, 0, 0, 0, 0, -1);

            final HubTestConfig.Setup setup = config.setup;
            log.info(String.format("S01 %s alice creates vault", setup));
            final ApiClient adminApiClient = getAdminApiClient(setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();
            assertNotNull(new HubStorageProfileListService(hubSession).list(Home.ROOT, new DisabledListProgressListener()).find(p -> StringUtils.equals(p.attributes().getFileId(),
                    config.vault.storageProfileId.toLowerCase())));

            final UUID vaultId = UUID.randomUUID();
            final boolean automaticAccessGrant = true;
            // upload template (STS: create bucket first, static: existing bucket)
            // TODO test with multiple wot levels?

            new CreateVaultService(hubSession).createVault(storageProfileWrapper,
                    new CreateVaultService.CreateVaultModel(vaultId,
                            "vault", null,
                            config.vault.storageProfileId, config.vault.username, config.vault.password, config.vault.bucketName,
                            config.vault.region, automaticAccessGrant, 3));
            checkNumberOfVaults(hubSession, config, vaultId, 0, 0, 1, 0, 0);

            log.info(String.format("S02 %s alice shares vault with admin as owner", setup));
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

            log.info(String.format("S03 %s admin uploads user keys", setup));
            final UserKeys adminKeys = UserKeys.create();
            final String adminAccountKey = config.setup.adminConfig.setupCode;
            final UsersResourceApi users = new UsersResourceApi(adminApiClient);

            // TODO https://github.com/shift7-ch/katta-server/issues/4 bad code smell - encapsulate initial setup
            final UserDto admin = users.apiUsersMeGet(false)
                    .ecdhPublicKey(adminKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(adminKeys.encodedEcdsaPublicKey())
                    .privateKey(adminKeys.encryptWithSetupCode(adminAccountKey))
                    .setupCode(new SetupCodeJWE(adminAccountKey).encryptForUser(adminKeys.ecdhKeyPair().getPublic()));
            users.apiUsersMePut(admin);
            checkNumberOfVaults(hubSession, config, vaultId, 1, 0, 1, 0, 1);

            log.info(String.format("S04 %s alice adds trust to admin", setup));
            new WoTServiceImpl(users).sign(new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), FirstLoginDeviceSetupCallback.disabled), admin);

            log.info(String.format("S04 %s alice grants access to admin", setup));
            new GrantAccessServiceImpl(hubSession).grantAccessToUsersRequiringAccessGrant(hubSession.getHost(), vaultId, FirstLoginDeviceSetupCallback.disabled);
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
