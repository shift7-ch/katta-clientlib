/*
 * Copyright (c) 2024 iterate GmbH. All rights reserved.
 */

package ch.iterate.hub.workflows;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
import ch.iterate.hub.core.callback.CreateVaultModel;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.SetupCodeJWE;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.model.StorageProfileDtoWrapperException;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestController;
import ch.iterate.hub.testsetup.HubTestUtilities;
import ch.iterate.hub.testsetup.docker_setup.UnattendedLocalOnly;
import ch.iterate.hub.testsetup.model.HubTestConfig;
import ch.iterate.hub.testsetup.model.HubTestSetupConfig;

import static ch.iterate.hub.testsetup.HubTestSetupConfigs.minioSTSUnattendedLocalOnly;
import static ch.iterate.hub.testsetup.HubTestSetupConfigs.minioStaticUnattendedLocalOnly;
import static ch.iterate.hub.testsetup.HubTestUtilities.getAdminApiClient;
import static ch.iterate.hub.testsetup.HubTestUtilities.setupForUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

/**
 * Create vault and share vault. Serves as hub API integration/regression test.
 * Local context (profiles, hub host collection) etc. is for user alice only.
 * Only remote hub calls are done for admin user.
 * Needs to be run separately for every storage profile because of the hard-coded vault counts.
 */
public class HubWorkflowTest {
    private static final Logger log = LogManager.getLogger(HubWorkflowTest.class.getName());

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({UnattendedLocalOnly.class})
    public class UnattendedLocalOnlySTS extends AbstractHubTest {
        private Stream<Arguments> arguments() {
            return Stream.of(minioSTSUnattendedLocalOnly);
        }

        @ParameterizedTest
        @MethodSource("arguments")
        public void testHubWorkflowSTS(final HubTestConfig hubTestConfig) throws Exception {
            testHubWorkflow(hubTestConfig);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @ExtendWith({UnattendedLocalOnly.class})
    public class UnattendedLocalOnlyStatic extends AbstractHubTest {

        private Stream<Arguments> arguments() {
            return Stream.of(minioStaticUnattendedLocalOnly);
        }

        @ParameterizedTest
        @MethodSource("arguments")
        public void testHubWorkflowStatic(final HubTestConfig hubTestConfig) throws Exception {
            testHubWorkflow(hubTestConfig);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedLocalOnlyStatic extends AbstractHubTest {
        @BeforeAll
        public void setup() {
            HubTestUtilities.preferences();
        }

        @ParameterizedTest
        @MethodSource("ch.iterate.hub.testsetup.HubTestSetupConfigs#provideAttendedLocalOnlyStatic")
        public void testHubWorkflowStatic(final HubTestConfig hubTestConfig) throws Exception {
            testHubWorkflow(hubTestConfig);
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    @Disabled("run standalone against already running hub")
    public class AttendedLocalOnlySTS extends AbstractHubTest {
        @BeforeAll
        public void setup() {
            HubTestUtilities.preferences();
        }

        @ParameterizedTest
        @MethodSource("ch.iterate.hub.testsetup.HubTestSetupConfigs#provideAttendedLocalOnlySTS")
        public void testHubWorkflowSTS(final HubTestConfig hubTestConfig) throws Exception {
            testHubWorkflow(hubTestConfig);
        }
    }

    public void testHubWorkflow(final HubTestConfig hubTestConfig) throws Exception {
        final HubSession hubSession = setupForUser(hubTestConfig.hubTestSetupConfig, hubTestConfig.hubTestSetupConfig.USER_001());

        checkNumberOfVaults(hubSession, hubTestConfig, null, 0, 0, 0, 0, -1);

        final HubTestSetupConfig hubTestSetupConfig = hubTestConfig.hubTestSetupConfig;
        log.info(String.format("S01 %s alice creates vault", hubTestSetupConfig));
        final ApiClient adminApiClient = getAdminApiClient(hubTestSetupConfig);
        final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
        log.info(storageProfiles);
        final StorageProfileDtoWrapper storageProfile = storageProfiles.stream()
                .map(StorageProfileDtoWrapper::coerce)
                .filter(p -> {
                    try {
                        return p.getId().toString().equals(hubTestConfig.vaultSpec.storageProfileId.toLowerCase());
                    }
                    catch(StorageProfileDtoWrapperException e) {
                        log.error(e);
                        return false;
                    }
                }).findFirst().get();

        final UUID vaultIdSharedWithAdmin = UUID.randomUUID();
        final boolean automaticAccessGrant = true;
        // upload template (STS: create bucket first, static: existing bucket)
        // TODO test with multiple wot levels?
        new CreateVaultService(hubSession, new HubTestController()).createVault(
                new CreateVaultModel(vaultIdSharedWithAdmin, "no reason", String.format("my first vault %s S01", storageProfile.getName()), "", storageProfile.getId().toString(), hubTestConfig.vaultSpec.username, hubTestConfig.vaultSpec.password, "handmade", storageProfile.getRegion(), automaticAccessGrant, 3));
        checkNumberOfVaults(hubSession, hubTestConfig, vaultIdSharedWithAdmin, 0, 0, 1, 0, 0);

        log.info(String.format("S02 %s alice shares vault with admin as owner", hubTestSetupConfig));
        final List<UserDto> userDtos = hubSession.getUsersApi().apiUsersGet();
        String adminId = null;
        for(UserDto user : userDtos) {
            if(user.getName().equals("admin")) {
                adminId = user.getId();
                break;
            }
        }
        hubSession.getVaultApi().apiVaultsVaultIdUsersUserIdPut(adminId, vaultIdSharedWithAdmin, Role.OWNER);
        checkNumberOfVaults(hubSession, hubTestConfig, vaultIdSharedWithAdmin, 1, 0, 1, 0, 0);

        log.info(String.format("S03 %s admin uploads user keys", hubTestSetupConfig));
        final UserKeys adminKeys = UserKeys.create();
        final String adminAccountKey = "blabla";
        final UsersResourceApi users = new UsersResourceApi(adminApiClient);

        // TODO https://github.com/shift7-ch/katta-server/issues/4 bad code smell - encapsulate initial setup
        final UserDto admin = users.apiUsersMeGet(false)
                .ecdhPublicKey(adminKeys.encodedEcdhPublicKey())
                .ecdsaPublicKey(adminKeys.encodedEcdsaPublicKey())
                .privateKey(adminKeys.encryptWithSetupCode(adminAccountKey))
                .setupCode(new SetupCodeJWE(adminAccountKey).encryptForUser(adminKeys.ecdhKeyPair().getPublic()));
        users.apiUsersMePut(admin);
        checkNumberOfVaults(hubSession, hubTestConfig, vaultIdSharedWithAdmin, 1, 0, 1, 0, 1);

        log.info(String.format("S04 %s alice adds trust to admin", hubTestSetupConfig));
        new CachingWoTService(users, new CachingUserKeysService(hubSession)).sign(admin);

        log.info(String.format("S04 %s alice grants access to admin", hubTestSetupConfig));
        new GrantAccessService(hubSession).grantAccessToUsersRequiringAccessGrant(vaultIdSharedWithAdmin);
        checkNumberOfVaults(hubSession, hubTestConfig, vaultIdSharedWithAdmin, 1, 0, 1, 0, 0);
    }

    private void checkNumberOfVaults(final HubSession hubSession, final HubTestConfig hubTestSetup, final UUID vaultIdSharedWithAdmin, final int adminOwner, final int adminMember, final int aliceOwner, final int aliceMember, final int nbUsersRequiringAccessGrant) throws ApiException, IOException {
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

        final ApiClient adminApiClient = getAdminApiClient(hubTestSetup.hubTestSetupConfig);
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
