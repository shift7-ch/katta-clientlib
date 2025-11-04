/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.workflows;

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.DisabledPasswordCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.UUIDRandomStringService;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3STSDto;
import cloud.katta.client.model.StorageProfileS3StaticDto;
import cloud.katta.crypto.uvf.UvfMetadataPayload;
import cloud.katta.crypto.uvf.VaultMetadataJWEAutomaticAccessGrantDto;
import cloud.katta.crypto.uvf.VaultMetadataJWEBackendDto;
import cloud.katta.model.StorageProfileDtoWrapper;
import cloud.katta.protocols.hub.HubSession;
import cloud.katta.protocols.hub.HubStorageLocationService;
import cloud.katta.protocols.hub.HubUVFVault;
import cloud.katta.protocols.hub.HubVaultRegistry;
import cloud.katta.testsetup.AbstractHubTest;
import cloud.katta.testsetup.HubTestConfig;
import cloud.katta.testsetup.HubTestUtilities;
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test01Bootstrapping(final HubTestConfig hubTestConfig) throws Exception {
        log.info("M01 {}", hubTestConfig);
        final String profile = hubTestConfig.setup.dockerConfig.profile;
        final Properties props = new Properties();
        props.load(this.getClass().getResourceAsStream(hubTestConfig.setup.dockerConfig.envFile));
        final HashMap<String, String> env = props.entrySet().stream().collect(
                Collectors.toMap(
                        e -> String.valueOf(e.getKey()),
                        e -> String.valueOf(e.getValue()),
                        (prev, next) -> next, HashMap::new
                ));

        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {

            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);

            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.registerModule(new JsonNullableModule());
            try {
                adminStorageProfileApi.apiStorageprofileS3staticPost(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(String.format("/setup/%s/aws_static/aws_static_profile.json", profile)), StorageProfileS3StaticDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD)
                );
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }
            try {
                adminStorageProfileApi.apiStorageprofileS3stsPost(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(String.format("/setup/%s/aws_sts/aws_sts_profile.json", profile)), StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD).bucketEncryption(S3SERVERSIDEENCRYPTION.NONE));
            }
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }
            try {
                final StorageProfileS3StaticDto storageProfile = mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(String.format("/setup/%s/minio_static/minio_static_profile.json", profile)), StorageProfileS3StaticDto.class)
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
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }
            try {
                final StorageProfileS3STSDto storageProfile = mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream(String.format("/setup/%s/minio_sts/minio_sts_profile.json", profile)), StorageProfileS3STSDto.class)
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
            catch(ApiException e) {
                if(e.getCode() == 409) {
                    log.warn(e);
                }
                else {
                    throw e;
                }
            }
            final List<StorageProfileDto> storageProfileDtos = new StorageProfileResourceApi(hubSession.getClient())
                    .apiStorageprofileGet(false);
            // aws static
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .startsWith("72736c19-283c-49d3-80a5-ab74b520254")));
            // aws sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .startsWith("844bd517-96d4-4787-bcfa-238e103149f")));
            // minio static
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .startsWith("71b910e0-2ecc-46de-a871-8db28549677")));
            // minio sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .startsWith("732d43fa-3716-46c4-b931-66ea5405ef1")));
        }
        catch(ApiException e) {
            log.error("{} {}", e.getCode(), e.getMessage(), e);
            throw e;
        }
        finally {
            hubSession.close();
        }
    }

    /**
     * Verify sync after adding new storage profile.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test02AddStorageProfile(final HubTestConfig hubTestConfig) throws Exception {
        log.info("M02 {}", hubTestConfig);

        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {
            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);
            final List<StorageProfileDto> storageProfiles = adminStorageProfileApi.apiStorageprofileGet(null);

            final UUID uuid = UUID.randomUUID();

            final StorageProfileDto storageProfile = storageProfiles.get(0);
            // client-generated code is not subclassed...
            if(storageProfile.getActualInstance() instanceof StorageProfileS3STSDto) {
                final StorageProfileS3STSDto profile = (StorageProfileS3STSDto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3stsPost(profile);
            } else if (storageProfile.getActualInstance() instanceof StorageProfileS3StaticDto) {
                final StorageProfileS3StaticDto profile = (StorageProfileS3StaticDto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3staticPost(profile);
            }
            else {
                fail();
            }
            assertEquals(storageProfiles.size() + 1, adminStorageProfileApi.apiStorageprofileGet(null).size());
        }
        finally {
            hubSession.close();
        }
    }

    /**
     * Create vaults for {MinIO S3,}x{STS,static} and list files encrypted and decrypted.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    void test03AddVault(final HubTestConfig config) throws Exception {
        log.info("M03 {}", config);

        final HubSession hubSession = setupConnection(config.setup);
        try {
            final ApiClient adminApiClient = getAdminApiClient(config.setup);
            final List<StorageProfileDto> storageProfiles = new StorageProfileResourceApi(adminApiClient).apiStorageprofileGet(false);
            log.info("Coercing storage profiles {}", storageProfiles);
            final StorageProfileDtoWrapper storageProfileWrapper = storageProfiles.stream()
                    .map(StorageProfileDtoWrapper::coerce)
                    .filter(p -> p.getId().toString().equals(config.vault.storageProfileId.toLowerCase())).findFirst().get();

            log.info("Creating vault in {}", hubSession);
            final UUID vaultId = UUID.fromString(new UUIDRandomStringService().random());

            final Path bucket = new Path(storageProfileWrapper.getBucketPrefix() + vaultId, EnumSet.of(Path.Type.volume, Path.Type.directory));
            final HubStorageLocationService.StorageLocation location = new HubStorageLocationService.StorageLocation(storageProfileWrapper.getId().toString(), storageProfileWrapper.getRegion(),
                    storageProfileWrapper.getName());
            final UvfMetadataPayload vaultMetadata = UvfMetadataPayload.create()
                    .withStorage(new VaultMetadataJWEBackendDto()
                            .username(config.vault.username)
                            .password(config.vault.password)
                            .provider(location.getProfile())
                            .defaultPath(bucket.getAbsolute())
                            .region(location.getRegion())
                            .nickname(null != bucket.attributes().getDisplayname() ? bucket.attributes().getDisplayname() : "Vault"))
                    .withAutomaticAccessGrant(new VaultMetadataJWEAutomaticAccessGrantDto()
                            .enabled(true)
                            .maxWotDepth(null));
            final HubUVFVault cryptomator = new HubUVFVault(new VaultServiceImpl(hubSession).getVaultStorageSession(hubSession, vaultId, vaultMetadata),
                    vaultId, vaultMetadata, new DisabledLoginCallback());
            cryptomator.create(hubSession, location.getIdentifier(), new VaultCredentials(StringUtils.EMPTY));

            final AttributedList<Path> vaults = hubSession.getFeature(ListService.class).list(Home.root(), new DisabledListProgressListener());
            assertFalse(vaults.isEmpty());

            final VaultRegistry vaultRegistry = hubSession.getRegistry();
            assertInstanceOf(HubVaultRegistry.class, vaultRegistry);
            {
                assertNotNull(vaults.find(new SimplePathPredicate(bucket)));
                assertTrue(hubSession.getFeature(Find.class).find(bucket));
                assertEquals(config.vault.region, hubSession.getFeature(AttributesFinder.class).find(bucket).getRegion());

                assertNotSame(Vault.DISABLED, vaultRegistry.find(hubSession, bucket));
            }

            final Path vault = vaults.find(new SimplePathPredicate(bucket));
            {
                // decrypted file listing
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertTrue(list.isEmpty());
            }
            {
                // encrypted file upload
                final Path file = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(234));
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(1, list.size());
                assertEquals(file.getName(), list.get(0).getName());
                assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
            }
            {
                // directory creation and listing
                final Path folder = new Path(vault, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
                hubSession.getFeature(Directory.class).mkdir(hubSession.getFeature(Write.class), folder, new TransferStatus());
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(2, list.size()); // a file and a folder
                {
                    // file upload in subfolder
                    final Path file = new Path(folder, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                    final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(555));
                    final AttributedList<Path> sublist = hubSession.getFeature(ListService.class).list(folder, new DisabledListProgressListener());
                    assertEquals(1, sublist.size());
                    assertEquals(file.getName(), sublist.get(0).getName());
                    assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                    // move operation to root folder and read again
                    hubSession.getFeature(Move.class).move(file, new Path(vault, file.getName(), EnumSet.of(Path.Type.file)), new TransferStatus(),
                            new Delete.DisabledCallback(), new DisabledConnectionCallback());
                    final AttributedList<Path> list2 = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                    assertEquals(3, list2.size()); // 1 subfolder and 2 files
                    assertEquals(1, list2.filter(Path::isDirectory).size());
                    assertEquals(2, list2.filter(Path::isFile).size());
                }
            }
            vaultRegistry.close(vault);
            assertFalse(vaultRegistry.contains(vault));
            assertSame(Vault.DISABLED, vaultRegistry.find(hubSession, vault));
            assertThrows(AccessDeniedException.class, () -> hubSession.getFeature(ListService.class).preflight(vault));
            assertThrows(NotfoundException.class, () -> hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener()));
        }
        finally {
            hubSession.close();
        }
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void test04SetupCode(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        assertEquals(OAuthTokens.EMPTY, hubSession.getHost().getCredentials().getOauth());
        assertEquals(StringUtils.EMPTY, hubSession.getHost().getCredentials().getPassword());
        final ListService feature = hubSession.getFeature(ListService.class);
        final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
        assertEquals(vaults, feature.list(Home.root(), new DisabledListProgressListener()));
        for(final Path vault : vaults) {
            assertTrue(hubSession.getFeature(Find.class).find(vault));
            final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
            assertEquals(3, list.size()); // 1 subfolder and 2 files
            assertEquals(1, list.toStream().filter(Path::isDirectory).count());
            assertEquals(2, list.toStream().filter(Path::isFile).count());
            for(Path f : list.filter(Path::isFile)) {
                final long length = f.attributes().getSize();
                HubTestUtilities.read(hubSession, f, (int) length);
            }
            for(Path d : list.filter(Path::isDirectory)) {
                {
                    // New file
                    final Path file = new Path(d, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.file));
                    final byte[] content = HubTestUtilities.write(hubSession, file, RandomUtils.nextBytes(247));
                    assertArrayEquals(content, HubTestUtilities.read(hubSession, file, content.length));
                    hubSession.getFeature(Delete.class).delete(Collections.singletonList(file), new DisabledPasswordCallback(), new Delete.DisabledCallback());
                    assertFalse(hubSession.getFeature(Find.class).find(file));
                }
                {
                    // New directory
                    final Path folder = new Path(d, new AlphanumericRandomStringService().random(), EnumSet.of(Path.Type.directory));
                    hubSession.getFeature(Directory.class).mkdir(hubSession.getFeature(Write.class), folder, new TransferStatus());
                    hubSession.getFeature(Delete.class).delete(Collections.singletonList(folder), new DisabledPasswordCallback(), new Delete.DisabledCallback());
                    assertFalse(hubSession.getFeature(Find.class).find(folder));
                }
                final AttributedList<Path> sublist = hubSession.getFeature(ListService.class).list(d, new DisabledListProgressListener());
                for(Path f : sublist.filter(Path::isFile)) {
                    final long length = f.attributes().getSize();
                    HubTestUtilities.read(hubSession, f, (int) length);
                }
            }
        }
    }
}
