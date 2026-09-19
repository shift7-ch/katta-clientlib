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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.JSON;
import cloud.katta.client.api.SettingsResourceApi;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.api.VaultResourceApi;
import cloud.katta.client.model.MemberDto;
import cloud.katta.client.model.Role;
import cloud.katta.client.model.S3StorageClass;
import cloud.katta.client.model.SettingsDto;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.crypto.AccountKeyPayload;
import cloud.katta.crypto.UserKeys;
import cloud.katta.crypto.uvf.UVFAccessTokenPayload;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubStorageLocationService;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestUtilities;
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The automatic access grant is callable by any vault member, not only by owners: a member holding an access token can
 * decrypt the vault and therefore re-share it. Adding members stays owner-gated and is not exercised here.
 */
abstract class AbstractHubMemberGrantTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubMemberGrantTest.class.getName());

    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void testGrantAccessAsMember(final HubTestConfig config) throws Exception {
        assertNotNull(config.setup.memberConfig, "Second regular test user required");
        try (final HubSession hubSession = setupConnection(config.setup.hubURL, config.setup.userConfig, config.vault)) {
            final ApiClient adminApiClient = getAdminApiClient(config.setup);
            final Properties env = new Properties();
            try (InputStream in = Objects.requireNonNull(this.getClass().getResourceAsStream(config.setup.dockerConfig.envFile))) {
                env.load(in);
            }

            // Grant to anyone. Verification of the Web of Trust itself is covered by the unit tests.
            log.info("Enable automatic access grant and disable WoT");
            final SettingsResourceApi settingsApi = new SettingsResourceApi(adminApiClient);
            final SettingsDto settings = settingsApi.apiSettingsGet();
            settings.setEnableAutomaticAccessGrant(true);
            settings.setAutomaticAccessGrantTrustThreshold(-1);
            settingsApi.apiSettingsPut(settings);

            log.info("S00 admin uploads storage profile");
            final StorageProfileResourceApi storageProfileApi = new StorageProfileResourceApi(adminApiClient);
            try (InputStream in = this.getClass().getResourceAsStream("/setup/minio_static/storage_profile.json")) {
                final String json = IOUtils.toString(Objects.requireNonNull(in), StandardCharsets.UTF_8)
                        .replace("${MINIO_SCHEME}", env.getProperty("MINIO_SCHEME"))
                        .replace("${MINIO_HOSTNAME}", env.getProperty("MINIO_HOSTNAME"))
                        .replace("${MINIO_PORT}", env.getProperty("MINIO_PORT"));
                final ObjectMapper mapper = new JSON().getMapper();
                storageProfileApi.apiStorageprofilePost(new StorageProfileDto(
                        mapper.readValue(json, StorageProfileS3StaticDto.class).storageClass(S3StorageClass.STANDARD)));
            }

            log.info("S01 alice creates vault");
            final StorageProfileDtoWrapper storageProfile = storageProfileApi.apiStorageprofileGet(false).stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(profile -> profile.getName().equals(config.vault.storageProfileName)).findFirst()
                    .orElseThrow(() -> new IllegalStateException(String.format("Storage profile %s not found", config.vault.storageProfileName)));
            final HubStorageLocationService.StorageLocation location = new HubStorageLocationService.StorageLocation(
                    storageProfile.getId().toString(), storageProfile.getRegion(), storageProfile.getName());
            final Vault cryptomator = hubSession.getFeature(VaultProvider.class).create(hubSession, location.getIdentifier(),
                    new Path(String.format("Vault %s", new AlphanumericRandomStringService().random()), EnumSet.of(Path.Type.volume, Path.Type.directory)),
                    new VaultVersion(VaultVersion.Type.UVF), new VaultCredentials());
            final UUID vaultId = UUID.fromString(StringUtils.removeStart(cryptomator.getHome().getName(), storageProfile.getBucketPrefix()));

            log.info("S02 admin and the second user set up their user keys");
            final UserKeys adminKeys = setupUserKeys(adminApiClient, config.setup.adminConfig);
            final ApiClient memberApiClient = HubTestUtilities.getApiClient(config.setup, config.setup.memberConfig);
            final UserKeys memberKeys = setupUserKeys(memberApiClient, config.setup.memberConfig);
            final String adminId = new UsersResourceApi(adminApiClient).apiUsersMeGet(false).getId();
            final String memberId = new UsersResourceApi(memberApiClient).apiUsersMeGet(false).getId();

            log.info("S03 alice adds admin as member of the vault and grants access, giving admin an access token");
            final VaultResourceApi aliceVaults = new VaultResourceApi(hubSession.getClient());
            aliceVaults.apiVaultsVaultIdUsersUserIdPut(adminId, vaultId, Role.MEMBER);
            final UserKeys aliceKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost(), hubSession.getMe()));
            new GrantAccessServiceImpl(hubSession).grantAccessToUsersRequiringAccessGrant(vaultId, aliceKeys);

            log.info("S04 alice adds the second user as member, leaving them without an access token");
            aliceVaults.apiVaultsVaultIdUsersUserIdPut(memberId, vaultId, Role.MEMBER);
            assertEquals(Collections.singletonList(memberId), aliceVaults.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId).stream()
                    .map(MemberDto::getId).collect(Collectors.toList()));

            final ApiException notGranted = assertThrows(ApiException.class,
                    () -> new VaultResourceApi(memberApiClient).apiVaultsVaultIdAccessTokenGet(vaultId, false));
            assertEquals(403, notGranted.getCode(), "No access token before the member grants one");

            log.info("S05 admin grants access although admin is only MEMBER of the vault");
            // None of the endpoints of the automatic access grant flow bypasses the vault role for the realm admin role
            new GrantAccessServiceImpl(new VaultResourceApi(adminApiClient), new UsersResourceApi(adminApiClient))
                    .grantAccessToUsersRequiringAccessGrant(adminKeys);

            assertTrue(aliceVaults.apiVaultsVaultIdUsersRequiringAccessGrantGet(vaultId).isEmpty(), "No pending access grants left");
            final UVFAccessTokenPayload granted = new VaultServiceImpl(new VaultResourceApi(memberApiClient)).getVaultAccessToken(vaultId, memberKeys);
            assertNotNull(granted.key());
            assertNull(granted.recoveryKey(), "Member-level access only");
        }
    }

    private static UserKeys setupUserKeys(final ApiClient client, final HubTestConfig.Setup.UserConfig config) throws Exception {
        final UserKeys keys = UserKeys.create();
        final UsersResourceApi users = new UsersResourceApi(client);
        users.apiUsersMePut(users.apiUsersMeGet(false)
                .ecdhPublicKey(keys.encodedEcdhPublicKey())
                .ecdsaPublicKey(keys.encodedEcdsaPublicKey())
                .privateKeys(keys.encryptWithAccountKey(config.setupCode))
                .setupCode(new AccountKeyPayload(config.setupCode).encryptForUser(keys.ecdhKeyPair().getPublic())));
        return keys;
    }
}
