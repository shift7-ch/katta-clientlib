/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultProvider;
import ch.cyberduck.core.vault.VaultVersion;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class AbstractHubWorkflowTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubWorkflowTest.class.getName());

    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void testHubWorkflow(final HubTestConfig testConfig) throws Exception {
        final HubSession hubSession = setupConnection(testConfig);
        try {
            checkNumberOfVaults(hubSession, testConfig, null, 0, 0, 0, 0, -1);

            final HubTestConfig.Setup setup = testConfig.setup;
            final ApiClient adminApiClient = getAdminApiClient(setup);
            final Properties props = new Properties();
            props.load(this.getClass().getResourceAsStream(testConfig.setup.dockerConfig.envFile));

            log.info("S00 admin uploads storage profile");
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.registerModule(new JsonNullableModule());
            {
                final StorageProfileS3StaticDto storageProfile = mapper.readValue(AbstractHubWorkflowTest.class.getResourceAsStream("/setup/local/minio_static/storage_profile.json"), StorageProfileS3StaticDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD);
                final String minioPort = props.getProperty("MINIO_PORT");
                if(minioPort != null) {
                    storageProfile.setPort(Integer.valueOf(minioPort));
                }
                final String minioHostname = props.getProperty("MINIO_HOSTNAME");
                if(minioHostname != null) {
                    storageProfile.setHostname(minioHostname);
                }
                adminStorageProfileApi.apiStorageprofileS3staticPost(storageProfile);
            }
            {
                final StorageProfileS3STSDto storageProfile = mapper.readValue(AbstractHubWorkflowTest.class.getResourceAsStream("/setup/local/minio_sts/storage_profile.json"), StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD)
                        .bucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
                final String minioPort = props.getProperty("MINIO_PORT");
                if(minioPort != null) {
                    storageProfile.setPort(Integer.valueOf(minioPort));
                    storageProfile.setStsEndpoint(storageProfile.getStsEndpoint().replace("9000", minioPort));
                }
                final String minioHostname = props.getProperty("MINIO_HOSTNAME");
                if(minioHostname != null) {
                    storageProfile.setStsEndpoint(storageProfile.getStsEndpoint().replace("minio", minioHostname));
                    storageProfile.setHostname(minioHostname);
                }
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

            log.info("S02 {} alice shares vault with admin as owner", setup);
            final List<UserDto> userDtos = new UsersResourceApi(adminApiClient).apiUsersGet().stream().map(UserKeysServiceImpl::withCountsToUserDto).collect(Collectors.toList());
            String adminId = userDtos.stream().filter(u -> "admin".equals(u.getName())).findFirst().get().getId();

            new VaultResourceApi(hubSession.getClient()).apiVaultsVaultIdUsersUserIdPut(adminId, vaultId, Role.OWNER);
            checkNumberOfVaults(hubSession, testConfig, vaultId, 1, 0, 1, 0, 0);

            log.info("S03 {} admin uploads user keys", setup);
            final UserKeys adminKeys = UserKeys.create();
            final String adminAccountKey = testConfig.setup.adminConfig.setupCode;
            final UsersResourceApi users = new UsersResourceApi(adminApiClient);

            final UserDto admin = users.apiUsersMeGet(false, false)
                    .ecdhPublicKey(adminKeys.encodedEcdhPublicKey())
                    .ecdsaPublicKey(adminKeys.encodedEcdsaPublicKey())
                    .privateKey(adminKeys.encryptWithAccountKey(adminAccountKey))
                    .setupCode(new AccountKeyPayload(adminAccountKey).encryptForUser(adminKeys.ecdhKeyPair().getPublic()));
            users.apiUsersMePut(admin);
            checkNumberOfVaults(hubSession, testConfig, vaultId, 1, 0, 1, 0, 1);

            log.info("S04 {} alice adds trust to admin", setup);
            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost(), hubSession.getMe()));
            new WoTServiceImpl(users).sign(userKeys, admin);

            log.info("S04 {} alice grants access to admin", setup);
            new GrantAccessServiceImpl(hubSession).grantAccessToUsersRequiringAccessGrant(vaultId, userKeys);

            checkNumberOfVaults(hubSession, testConfig, vaultId, 1, 0, 1, 0, 0);
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
