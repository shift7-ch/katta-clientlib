/*
 * Copyright (c) 2025 shift7 GmbH. All rights reserved.
 */

package ch.iterate.hub.core;

import ch.cyberduck.core.*;
import ch.cyberduck.core.exception.BackgroundException;
import ch.cyberduck.core.features.Bulk;
import ch.cyberduck.core.features.Delete;
import ch.cyberduck.core.features.Directory;
import ch.cyberduck.core.features.Home;
import ch.cyberduck.core.features.Move;
import ch.cyberduck.core.features.Read;
import ch.cyberduck.core.features.Vault;
import ch.cyberduck.core.features.Write;
import ch.cyberduck.core.io.StatusOutputStream;
import ch.cyberduck.core.preferences.PreferencesFactory;
import ch.cyberduck.core.proxy.DisabledProxyFinder;
import ch.cyberduck.core.ssl.DefaultX509KeyManager;
import ch.cyberduck.core.ssl.DisabledX509TrustManager;
import ch.cyberduck.core.transfer.Transfer;
import ch.cyberduck.core.transfer.TransferItem;
import ch.cyberduck.core.transfer.TransferStatus;
import ch.cyberduck.core.vault.DefaultVaultRegistry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openapitools.jackson.nullable.JsonNullableModule;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import static ch.iterate.hub.testsetup.HubTestUtilities.getAdminApiClient;
import static org.junit.jupiter.api.Assertions.*;

import ch.iterate.hub.client.ApiClient;
import ch.iterate.hub.client.ApiException;
import ch.iterate.hub.client.api.StorageProfileResourceApi;
import ch.iterate.hub.client.model.S3SERVERSIDEENCRYPTION;
import ch.iterate.hub.client.model.S3STORAGECLASSES;
import ch.iterate.hub.client.model.StorageProfileDto;
import ch.iterate.hub.client.model.StorageProfileS3Dto;
import ch.iterate.hub.client.model.StorageProfileS3STSDto;
import ch.iterate.hub.crypto.UserKeys;
import ch.iterate.hub.model.StorageProfileDtoWrapper;
import ch.iterate.hub.protocols.hub.HubSession;
import ch.iterate.hub.protocols.hub.HubStorageProfileListService;
import ch.iterate.hub.protocols.hub.HubStorageProfileSyncSchedulerService;
import ch.iterate.hub.protocols.hub.HubStorageVaultSyncSchedulerService;
import ch.iterate.hub.protocols.s3.S3AutoLoadVaultSession;
import ch.iterate.hub.testsetup.AbstractHubTest;
import ch.iterate.hub.testsetup.HubTestConfig;
import ch.iterate.hub.testsetup.MethodIgnorableSource;
import ch.iterate.hub.workflows.CreateVaultService;
import ch.iterate.hub.workflows.DeviceKeysServiceImpl;
import ch.iterate.hub.workflows.UserKeysServiceImpl;
import ch.iterate.hub.workflows.VaultServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractHubSynchronizeTest extends AbstractHubTest {
    private static final Logger log = LogManager.getLogger(AbstractHubSynchronizeTest.class.getName());

    /**
     * Start with unattended setup (e.g. UnattendedMinio) and then run tests with corresponding attended setup (e.g. AttendedMinio) to save startup times at every test execution.
     */
    @Test
    @Disabled
    public void startUnattendedSetupToUseAttended() throws InterruptedException {
        log.info("Unattended setup ready to be used in attended test runs.");
        // run forever
        Thread.sleep(924982347);
    }

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

            // trigger blocking sync
            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());

            log.info("{} Protocols found:", ProtocolFactory.get().find().size());
            for(final Protocol protocol : ProtocolFactory.get().find()) {
                log.info("-  {}", protocol);
            }

            // aws static
            assertNotNull(ProtocolFactory.get().forName("72736C19-283C-49D3-80A5-AB74B5202543".toLowerCase()));
            // aws sts
            assertNotNull(ProtocolFactory.get().forName("844BD517-96D4-4787-BCFA-238E103149F6".toLowerCase()));
            // minio static
            assertNotNull(ProtocolFactory.get().forName("71B910E0-2ECC-46DE-A871-8DB28549677E".toLowerCase()));
            // minio sts
            assertNotNull(ProtocolFactory.get().forName("732D43FA-3716-46C4-B931-66EA5405EF1C".toLowerCase()));
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

            // trigger blocking sync
            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());
            final int numProtocols = ProtocolFactory.get().find().size();

            log.info("Add storage profile for UUID {}", uuid);
            assertNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));

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

            new HubStorageProfileSyncSchedulerService(hubSession).operate(new DisabledLoginCallback());

            assertEquals(numProtocols + 1, ProtocolFactory.get().find().size());
            assertNotNull(ProtocolFactory.get().forName(uuid.toString().toLowerCase()));
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
            assertNotNull(new HubStorageProfileListService(hubSession).list(Home.ROOT, new DisabledListProgressListener()).find(p -> StringUtils.equals(p.attributes().getFileId(),
                    config.vault.storageProfileId.toLowerCase())));

            log.info("Creating vault in {}", hubSession);
            final UUID vaultId = UUID.randomUUID();
            final UserKeys userKeys = new UserKeysServiceImpl(hubSession).getUserKeys(hubSession.getHost(), hubSession.getMe(),
                    new DeviceKeysServiceImpl().getDeviceKeys(hubSession.getHost()));
            new CreateVaultService(hubSession).createVault(userKeys, storageProfileWrapper, new CreateVaultService.CreateVaultModel(
                    vaultId, "vault", null,
                    config.vault.storageProfileId, config.vault.username, config.vault.password, config.vault.bucketName, config.vault.region, true, 3));
            log.info("Getting vault bookmark for vault {}", vaultId);
            final Host vaultBookmark = HubStorageVaultSyncSchedulerService.toBookmark(hubSession.getHost(), vaultId,
                    new VaultServiceImpl(hubSession).getVaultMetadataJWE(vaultId, userKeys).storage());
            log.info("Using vault bookmark {}", vaultBookmark);

            final DefaultVaultRegistry vaultRegistry = new DefaultVaultRegistry(new DisabledPasswordCallback());
            final Session<?> session = new S3AutoLoadVaultSession(vaultBookmark, new DisabledX509TrustManager(), new DefaultX509KeyManager())
                    .withRegistry(vaultRegistry);
            session.open(new DisabledProxyFinder(), new DisabledHostKeyCallback(), new DisabledLoginCallback(), new DisabledCancelCallback());
            session.login(new DisabledLoginCallback(), new DisabledCancelCallback());

            // listing decrypted file names
            assertFalse(vaultRegistry.isEmpty());
            assertEquals(1, vaultRegistry.size());

            final Path bucket = new Path(vaultBookmark.getDefaultPath(), EnumSet.of(Path.Type.directory, Path.Type.volume, Path.Type.vault));
            assertNotSame(Vault.DISABLED, vaultRegistry.find(session, bucket));

            {
                // encrypted file listing
                final AttributedList<Path> list = session.getFeature(ListService.class).list(bucket, new DisabledListProgressListener());
                assertTrue(list.isEmpty());
            }
            {
                // encrypted file upload
                final Path home = vaultRegistry.find(session, bucket).getHome();
                final Path file = new Path(home, new AlphanumericRandomStringService(25).random(), EnumSet.of(AbstractPath.Type.file));
                byte[] content = writeRandomFile(session, file, 234);
                final AttributedList<Path> list = session.getFeature(ListService.class).list(bucket, new DisabledListProgressListener());
                assertEquals(1, list.size());
                assertEquals(file.getName(), list.get(0).getName());

                byte[] actual = new byte[300];
                try (final InputStream inputStream = session.getFeature(Read.class).read(file, new TransferStatus(), new DisabledConnectionCallback())) {
                    int l = inputStream.read(actual);
                    assert l == 234;
                    assertArrayEquals(content, Arrays.copyOfRange(actual, 0, l));
                }
            }
            {
                // encrypted directory creation and listing
                final Path home = vaultRegistry.find(session, bucket).getHome();
                final Path folder = new Path(home, new AlphanumericRandomStringService(25).random(), EnumSet.of(AbstractPath.Type.directory));

                session.getFeature(Directory.class).mkdir(folder, new TransferStatus());
                final AttributedList<Path> list = session.getFeature(ListService.class).list(bucket, new DisabledListProgressListener());
                assertEquals(2, list.size()); // a file and a folder

                {
                    // encrypted file upload in subfolder
                    final Path file = new Path(folder, new AlphanumericRandomStringService(25).random(), EnumSet.of(AbstractPath.Type.file));
                    final byte[] content = writeRandomFile(session, file, 555);
                    final AttributedList<Path> sublist = session.getFeature(ListService.class).list(folder, new DisabledListProgressListener());
                    assertEquals(1, sublist.size());
                    assertEquals(file.getName(), sublist.get(0).getName());

                    byte[] actual = new byte[600];
                    try (final InputStream inputStream = session.getFeature(Read.class).read(file, new TransferStatus(), new DisabledConnectionCallback())) {
                        int l = inputStream.read(actual);
                        assert l == 555;
                        assertArrayEquals(content, Arrays.copyOfRange(actual, 0, l));
                    }

                    // move operation to root folder and read again
                    session.getFeature(Move.class).move(file, new Path(home, file.getName(), EnumSet.of(AbstractPath.Type.file)), new TransferStatus(), new Delete.DisabledCallback(), new DisabledConnectionCallback());

                    final AttributedList<Path> list2 = session.getFeature(ListService.class).list(home, new DisabledListProgressListener());
                    assertEquals(3, list2.size()); // 1 subfolder and 2 files

                    assertEquals(1, list2.toStream().map(Path::isDirectory).filter(Boolean::booleanValue).count());
                    assertEquals(2, list2.toStream().map(Path::isFile).filter(Boolean::booleanValue).count());
                }
            }
            {
                // raw listing encrypted file names
                // aka. ciphertext directory structure
                //  see https://github.com/encryption-alliance/unified-vault-format/blob/develop/file%20name%20encryption/AES-SIV-512-B64URL.md#ciphertext-directory-structure
                vaultRegistry.close(bucket);
                assertSame(Vault.DISABLED, vaultRegistry.find(session, bucket));
                assertTrue(vaultRegistry.isEmpty());

                {
                    final AttributedList<Path> list = session.getFeature(ListService.class).list(bucket, new DisabledListProgressListener());
                    assertFalse(list.isEmpty());
                    assertEquals(2, list.size());
                    // /<bucket>/d/
                    assertNotNull(list.find(new SimplePathPredicate(new Path(bucket, "d", EnumSet.of(Path.Type.directory, AbstractPath.Type.placeholder)))));
                    // /<bucket>/vault.uvf
                    assertNotNull(list.find(new SimplePathPredicate(new Path(bucket, PreferencesFactory.get().getProperty("cryptomator.vault.config.filename"), EnumSet.of(Path.Type.file)))));
                }
                {
                    // level 2: /<bucket>/d/
                    final AttributedList<Path> level2List = session.getFeature(ListService.class).list(new Path(bucket, "d", EnumSet.of(Path.Type.directory, AbstractPath.Type.placeholder)), new DisabledListProgressListener());
                    assertFalse(level2List.isEmpty());
                    assertEquals(2, level2List.size());
                    for(final Path level3 : level2List) {
                        // level 3: /<bucket>/d/<2-letter-folder>/
                        final AttributedList<Path> level3List = session.getFeature(ListService.class).list(level3, new DisabledListProgressListener());
                        // by hashing, only 1 sub-folder expected
                        assertEquals(1, level3List.size());
                        for(final Path level4 : level3List) {
                            // level 4: /<bucket>/d/<2-letter-folder>/<30-letter-folder/
                            final AttributedList<Path> level4List = session.getFeature(ListService.class).list(level4, new DisabledListProgressListener());
                            assertTrue(level4List.toStream().map(Path::getName).allMatch(n -> n.endsWith(".uvf")));
                            // empty sub-folder
                            log.info("level4List.size()={}", level4List.size());
                            assert (level4List.size() >= 2);
                            // root folder contains two files and a sub-folder
                            assertTrue(level4List.size() <= 3);
                            if(level4List.size() == 2) {
                                // MiniO versioned API returns a first version with the file content and a second empty version upon deletion
                                assertTrue(level4List.toStream().allMatch(p -> p.attributes().isDuplicate()));
                            }
                            else if(level4List.size() == 3) {
                                // the root directory -> contains two files...
                                assertEquals(2, level4List.toStream().map(p -> p.isFile() && p.getName().endsWith(".uvf")).filter(Boolean::booleanValue).count());
                                assertEquals(1, level4List.toStream().map(p -> p.isDirectory() && p.getName().endsWith(".uvf")).filter(Boolean::booleanValue).count());
                                // ... and a subfolder with a dir.uvf in it
                                final Path level5 = level4List.toStream().filter(Path::isDirectory).findFirst().get();
                                final AttributedList<Path> level5list = session.getFeature(ListService.class).list(level5, new DisabledListProgressListener());
                                assertEquals(1, level5list.size());
                                final Path level6 = level5list.get(0);
                                assertEquals("dir.uvf", level6.getName());
                                assertTrue(level6.isFile());
                            }
                        }
                    }
                }
            }
        }
        finally {
            hubSession.close();
        }
    }

    private static byte @NotNull [] writeRandomFile(final Session<?> session, final Path file, int size) throws BackgroundException, IOException {
        final byte[] content = RandomUtils.nextBytes(size);
        final TransferStatus transferStatus = new TransferStatus().withLength(content.length);
        transferStatus.setChecksum(session.getFeature(Write.class).checksum(file, transferStatus).compute(new ByteArrayInputStream(content), transferStatus));
        session.getFeature(Bulk.class).pre(Transfer.Type.upload, Collections.singletonMap(new TransferItem(file), transferStatus), new DisabledConnectionCallback());
        final StatusOutputStream<?> out = session.getFeature(Write.class).write(file, transferStatus, new DisabledConnectionCallback());
        IOUtils.copyLarge(new ByteArrayInputStream(content), out);
        out.close();
        return content;
    }
}
