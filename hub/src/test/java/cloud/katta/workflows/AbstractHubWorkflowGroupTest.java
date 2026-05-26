/*
 * Copyright (c) 2026 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.GroupsResourceApi;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.CreateGroupDto;
import cloud.katta.client.model.GroupDto;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.client.model.UserDto;
import cloud.katta.client.model.VaultDto;
import cloud.katta.crypto.AccountKeyPayload;
import cloud.katta.crypto.UserKeys;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubStorageLocationService;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class AbstractHubWorkflowGroupTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubWorkflowGroupTest.class.getName());

    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void testHubWorkflowShareVaultWithGroup(final HubTestConfig testConfig) throws Exception {
        try (final HubSession hubSession = setupConnection(testConfig.setup.hubURL, testConfig.setup.userConfig, testConfig.vault)) {
            checkNumberOfVaults(hubSession, testConfig, null, 0, 0, 0, 0, -1);

            final HubTestConfig.Setup setup = testConfig.setup;
            final ApiClient adminApiClient = getAdminApiClient(setup);
            final Properties configuration = new Properties();
            final HubTestConfig.Setup.DockerConfig dockerConfig = testConfig.setup.dockerConfig;
            try (InputStream in = Objects.requireNonNull(this.getClass().getResourceAsStream(dockerConfig.envFile))) {
                configuration.load(in);
            }

            log.info("S00 admin uploads storage profile");
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            final ObjectMapper mapper = new JSON().getMapper();
            try (InputStream in = this.getClass().getResourceAsStream("/setup/minio_static/storage_profile.json")) {
                final String json = IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8)
                        .replace("${MINIO_SCHEME}", configuration.getProperty("MINIO_SCHEME"))
                        .replace("${MINIO_HOSTNAME}", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("${MINIO_PORT}", configuration.getProperty("MINIO_PORT"));
                final StorageProfileS3StaticDto storageProfile = mapper.readValue(json, StorageProfileS3StaticDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD);
                adminStorageProfileApi.apiStorageprofileS3staticPost(storageProfile);
            }
            try (InputStream in = this.getClass().getResourceAsStream("/setup/minio_sts/storage_profile.json")) {
                final String json = IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8)
                        .replace("${MINIO_SCHEME}", configuration.getProperty("MINIO_SCHEME"))
                        .replace("${MINIO_HOSTNAME}", configuration.getProperty("MINIO_HOSTNAME"))
                        .replace("${MINIO_PORT}", configuration.getProperty("MINIO_PORT"));
                final StorageProfileS3STSDto storageProfile = mapper.readValue(json, StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD)
                        .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
                adminStorageProfileApi.apiStorageprofileS3stsPost(storageProfile);
            }

            log.info("S01 {} alice creates vault", setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(testConfig.vault.storageProfileId.toLowerCase())).findFirst().get();

            final Path vaultName = new Path(String.format("Vault %s", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.volume, Path.Type.directory));
            final HubStorageLocationService.StorageLocation location = new HubStorageLocationService.StorageLocation(storageProfileWrapper.getId().toString(), storageProfileWrapper.getRegion(),
                    storageProfileWrapper.getName());

            final VaultProvider vaultProvider = hubSession.getFeature(VaultProvider.class);
            final Vault cryptomator = vaultProvider.create(hubSession, location.getIdentifier(), vaultName, new VaultVersion(VaultVersion.Type.UVF), new VaultCredentials());

            final UUID vaultId = UUID.fromString(StringUtils.removeStart(cryptomator.getHome().getName(), storageProfileWrapper.getBucketPrefix()));
            checkNumberOfVaults(hubSession, testConfig, vaultId, 0, 0, 1, 0, 0);

            log.info("S02 {} admin creates group and adds admin user to it", setup);
            final UsersResourceApi usersApi = new UsersResourceApi(adminApiClient);
            final List<UserDto> userDtos = new UsersResourceApi(adminApiClient).apiUsersGet().stream().map(UserKeysServiceImpl::withCountsToUserDto).collect(Collectors.toList());
            final String adminId = userDtos.stream().filter(u -> "admin".equals(u.getName())).findFirst().get().getId();

            final GroupsResourceApi adminGroupsApi = new GroupsResourceApi(adminApiClient);
            final String groupName = String.format("Group %s", new AlphanumericRandomStringService().random());
            adminGroupsApi.apiGroupsPost(new CreateGroupDto().name(groupName));
            final List<GroupDto> groups = adminGroupsApi.apiGroupsGet();
            final String groupId = groups.stream().filter(g -> groupName.equals(g.getName())).findFirst().get().getId();
            log.info("Created group {} with id {}", groupName, groupId);

            adminGroupsApi.apiGroupsGroupIdMembersUserIdPost(groupId, adminId);
            log.info("Added admin to group {}", groupId);

            log.info("S03 {} alice shares vault with group as MEMBER", setup);
            final VaultResourceApi aliceVaultApi = new VaultResourceApi(hubSession.getClient());
            aliceVaultApi.apiVaultsVaultIdGroupsGroupIdPut(groupId, vaultId, Role.MEMBER);
            checkNumberOfVaults(hubSession, testConfig, vaultId, 0, 1, 1, 0, 0);

            log.info("S04 {} admin uploads user keys", setup);
            final UserKeys adminKeys = UserKeys.create();
            final String adminAccountKey = testConfig.setup.adminConfig.setupCode;

            final UserDto admin = usersApi.apiUsersMeGet(false, false)
                    .ecdhPublicKey(adminKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(adminKeys.encodedEcdsaPublicKey())
                    .privateKey(adminKeys.encryptWithAccountKey(adminAccountKey))
                    .setupCode(new AccountKeyPayload(adminAccountKey).encryptForUser(adminKeys.ecdhKeyPair().getPublic()));
            usersApi.apiUsersMePut(admin);
            checkNumberOfVaults(hubSession, testConfig, vaultId, 0, 1, 1, 0, 1);

            log.info("S05 {} alice adds trust to admin", setup);
            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost(), hubSession.getMe()));
            new WoTServiceImpl(new UsersResourceApi(hubSession.getClient())).sign(userKeys, admin);

            log.info("S06 {} alice grants access to admin", setup);
            new GrantAccessServiceImpl(hubSession).grantAccessToUsersRequiringAccessGrant(vaultId, userKeys);

            checkNumberOfVaults(hubSession, testConfig, vaultId, 0, 1, 1, 0, 0);
        }
    }

    private static void checkNumberOfVaults(final HubSession hubSession, final HubTestConfig hubTestSetup, final UUID vaultIdSharedWithGroup,
                                            final int adminOwner, final int adminMember, final int aliceOwner, final int aliceMember, final int nbUsersRequiringAccessGrant) throws ApiException, IOException {
        final VaultResourceApi vaultResourceApiAlice = new VaultResourceApi(hubSession.getClient());
        final List<VaultDto> vaultAliceOwned = vaultResourceApiAlice.apiVaultsAccessibleGet(Role.OWNER);
        for(final VaultDto vaultDto : vaultAliceOwned) {
            log.info("owned by alice {}", vaultDto);
        }
        assertEquals(aliceOwner, vaultAliceOwned.size(), "alice OWNER");

        final List<VaultDto> vaultAliceMember = vaultResourceApiAlice.apiVaultsAccessibleGet(Role.MEMBER);
        for(final VaultDto vaultDto : vaultAliceMember) {
            log.info("alice member of {}", vaultDto);
        }
        assertEquals(aliceMember, vaultAliceMember.size(), "alice MEMBER");

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
            final List<MemberDto> usersRequiringAccessGrant = vaultResourceApiAlice.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultIdSharedWithGroup);
            for(final MemberDto memberDto : usersRequiringAccessGrant) {
                log.info("user requiring access to {}: {}", vaultIdSharedWithGroup, memberDto);
            }
            assertEquals(nbUsersRequiringAccessGrant, usersRequiringAccessGrant.size());
        }
    }
}
