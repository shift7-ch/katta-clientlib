/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package cloud.katta.core;

import ch.cyberduck.core.AlphanumericRandomStringService;
import ch.cyberduck.core.AttributedList;
import ch.cyberduck.core.DisabledConnectionCallback;
import ch.cyberduck.core.DisabledListProgressListener;
import ch.cyberduck.core.DisabledLoginCallback;
import ch.cyberduck.core.ListService;
import ch.cyberduck.core.OAuthTokens;
import ch.cyberduck.core.Path;
import ch.cyberduck.core.ProtocolFactory;
import ch.cyberduck.core.Session;
import ch.cyberduck.core.SimplePathPredicate;
import ch.cyberduck.core.UUIDRandomStringService;
import ch.cyberduck.core.exception.AccessDeniedException;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.exception.NotfoundException;
import ch.cyberduck.core.features.AttributesFinder;
import ch.cyberduck.core.features.Bulk;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Find;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.io.StatusOutputStream;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.VaultCredentials;
import ch.cyberduck.core.vault.VaultRegistry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import cloud.katta.client.ApiClient;
import cloud.katta.client.ApiException;
import cloud.katta.client.api.StorageProfileResourceApi;
import cloud.katta.client.api.UsersResourceApi;
import cloud.katta.client.model.S3SERVERSIDEENCRYPTION;
import cloud.katta.client.model.S3STORAGECLASSES;
import cloud.katta.client.model.StorageProfileDto;
import cloud.katta.client.model.StorageProfileS3Dto;
import cloud.katta.client.model.StorageProfileS3STSDto;
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
import cloud.katta.testsetup.MethodIgnorableSource;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import static cloud.katta.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    /**
     * Verify storage profiles are synced from hub bookmark.
     */
    @ParameterizedTest
    @MethodIgnorableSource(value = "arguments")
    public void test01Bootstrapping(final HubTestConfig hubTestConfig) throws Exception {
        log.info("M01 {}", hubTestConfig);

        final HubSession hubSession = setupConnection(hubTestConfig.setup);
        try {

            final ApiClient adminApiClient = getAdminApiClient(hubTestConfig.setup);
            final StorageProfileResourceApi adminStorageProfileApi = new StorageProfileResourceApi(adminApiClient);

            final ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            mapper.registerModule(new JsonNullableModule());
            try {
                adminStorageProfileApi.apiStorageprofileS3Put(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/aws_static/aws_static_profile.json"), StorageProfileS3Dto.class).storageClass(S3STORAGECLASSES.STANDARD));
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
                adminStorageProfileApi.apiStorageprofileS3stsPut(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/aws_sts/aws_sts_profile.json"), StorageProfileS3STSDto.class)
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
                adminStorageProfileApi.apiStorageprofileS3Put(mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/minio_static/minio_static_profile.json"), StorageProfileS3Dto.class).storageClass(S3STORAGECLASSES.STANDARD));
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
                final StorageProfileS3STSDto storageProfile = mapper.readValue(AbstractHubSynchronizeTest.class.getResourceAsStream("/setup/minio_sts/minio_sts_profile.json"), StorageProfileS3STSDto.class)
                        .storageClass(S3STORAGECLASSES.STANDARD).bucketEncryption(S3SERVERSIDEENCRYPTION.NONE);
                adminStorageProfileApi.apiStorageprofileS3stsPut(storageProfile);
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
                    .equals("72736c19-283c-49d3-80a5-ab74b5202543")));
            // aws sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .equals("844bd517-96d4-4787-bcfa-238e103149f6")));
            // minio static
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .equals("71b910e0-2ecc-46de-a871-8db28549677e")));
            // minio sts
            assertTrue(storageProfileDtos.stream().anyMatch(storageProfileDto -> StorageProfileDtoWrapper.coerce(storageProfileDto).getId().toString()
                    .equals("732d43fa-3716-46c4-b931-66ea5405ef1c")));
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
    public void test02AddStorageProfile(final HubTestConfig hubTestConfig) throws Exception {
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
                adminStorageProfileApi.apiStorageprofileS3stsPut(profile);
            }
            else if(storageProfile.getActualInstance() instanceof StorageProfileS3Dto) {
                final StorageProfileS3Dto profile = (StorageProfileS3Dto) storageProfile.getActualInstance();
                profile.setId(uuid);
                adminStorageProfileApi.apiStorageprofileS3Put(profile);
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
    public void test03AddVault(final HubTestConfig config) throws Exception {
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

            final Path bucket = new Path(null == config.vault.bucketName ? null == storageProfileWrapper.getBucketPrefix() ? "katta-test-" + vaultId : storageProfileWrapper.getBucketPrefix() + vaultId : config.vault.bucketName,
                    EnumSet.of(Path.Type.volume, Path.Type.directory));
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
            final HubUVFVault cryptomator = new HubUVFVault(hubSession.getFeature(ProtocolFactory.class).forName(location.getProfile()),
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
                final Path file = new Path(vault, new AlphanumericRandomStringService(25).random(), EnumSet.of(Path.Type.file));
                byte[] content = writeRandomFile(hubSession, file, 234);
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(1, list.size());
                assertEquals(file.getName(), list.get(0).getName());

                byte[] actual = new byte[300];
                try (final InputStream inputStream = hubSession.getFeature(Read.class).read(file, new TransferStatus(), new DisabledConnectionCallback())) {
                    int l = inputStream.read(actual);
                    assert l == 234;
                    assertArrayEquals(content, Arrays.copyOfRange(actual, 0, l));
                }
            }
            {
                // directory creation and listing
                final Path folder = new Path(vault, new AlphanumericRandomStringService(25).random(), EnumSet.of(Path.Type.directory));

                hubSession.getFeature(Directory.class).mkdir(hubSession.getFeature(Write.class), folder, new TransferStatus());
                final AttributedList<Path> list = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                assertEquals(2, list.size()); // a file and a folder

                {
                    // file upload in subfolder
                    final Path file = new Path(folder, new AlphanumericRandomStringService(25).random(), EnumSet.of(Path.Type.file));
                    final byte[] content = writeRandomFile(hubSession, file, 555);
                    final AttributedList<Path> sublist = hubSession.getFeature(ListService.class).list(folder, new DisabledListProgressListener());
                    assertEquals(1, sublist.size());
                    assertEquals(file.getName(), sublist.get(0).getName());

                    byte[] actual = new byte[600];
                    try (final InputStream inputStream = hubSession.getFeature(Read.class).read(file, new TransferStatus(), new DisabledConnectionCallback())) {
                        int l = inputStream.read(actual);
                        assert l == 555;
                        assertArrayEquals(content, Arrays.copyOfRange(actual, 0, l));
                    }

                    // move operation to root folder and read again
                    hubSession.getFeature(Move.class).move(file, new Path(vault, file.getName(), EnumSet.of(Path.Type.file)), new TransferStatus(), new Delete.DisabledCallback(), new DisabledConnectionCallback());

                    final AttributedList<Path> list2 = hubSession.getFeature(ListService.class).list(vault, new DisabledListProgressListener());
                    assertEquals(3, list2.size()); // 1 subfolder and 2 files

                    assertEquals(1, list2.toStream().map(Path::isDirectory).filter(Boolean::booleanValue).count());
                    assertEquals(2, list2.toStream().map(Path::isFile).filter(Boolean::booleanValue).count());
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
    public void test04SetupCode(final HubTestConfig config) throws Exception {
        final HubSession hubSession = setupConnection(config.setup);
        assertEquals(OAuthTokens.EMPTY, hubSession.getHost().getCredentials().getOauth());
        assertEquals(StringUtils.EMPTY, hubSession.getHost().getCredentials().getPassword());
        final ListService feature = hubSession.getFeature(ListService.class);
        final AttributedList<Path> vaults = feature.list(Home.root(), new DisabledListProgressListener());
        assertEquals(vaults, feature.list(Home.root(), new DisabledListProgressListener()));
        for(final Path vault : vaults) {
            assertTrue(hubSession.getFeature(Find.class).find(vault));
        }
        new UsersResourceApi(hubSession.getClient()).apiUsersMeGet(true, false);
    }

    private static byte @NotNull [] writeRandomFile(final Session<?> session, final Path file, int size) throws BackgroundException, IOException {
        final byte[] content = RandomUtils.nextBytes(size);
        final TransferStatus transferStatus = new TransferStatus().setLength(content.length);
        transferStatus.setChecksum(session.getFeature(Write.class).checksum(file, transferStatus).compute(new ByteArrayInputStream(content), transferStatus));
        session.getFeature(Bulk.class).pre(Transfer.Type.upload, Collections.singletonMap(new TransferItem(file), transferStatus), new DisabledConnectionCallback());
        final StatusOutputStream<?> out = session.getFeature(Write.class).write(file, transferStatus, new DisabledConnectionCallback());
        IOUtils.copyLarge(new ByteArrayInputStream(content), out);
        out.close();
        return content;
    }
}
